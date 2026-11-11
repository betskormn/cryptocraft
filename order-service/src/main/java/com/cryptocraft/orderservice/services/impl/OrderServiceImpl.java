package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.clients.InventoryClient;
import com.cryptocraft.orderservice.clients.PricingClient;
import com.cryptocraft.orderservice.clients.dto.CurrentPriceDto;
import com.cryptocraft.orderservice.clients.dto.SellableItemDto;
import com.cryptocraft.orderservice.controllers.dto.request.PlaceOrderRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.OrderResponseDto;
import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.dao.OrderLine;
import com.cryptocraft.orderservice.dao.SessionParticipant;
import com.cryptocraft.orderservice.dao.TableSession;
import com.cryptocraft.orderservice.dao.enums.ItemType;
import com.cryptocraft.orderservice.dao.enums.OrderStatus;
import com.cryptocraft.orderservice.dao.enums.SessionStatus;
import com.cryptocraft.orderservice.dao.enums.StockStatus;
import com.cryptocraft.orderservice.events.OrderPlacedEvent;
import com.cryptocraft.orderservice.exceptions.OrderRejectedException;
import com.cryptocraft.orderservice.mappers.OrderMapper;
import com.cryptocraft.orderservice.repositories.OrderRepository;
import com.cryptocraft.orderservice.repositories.SessionParticipantRepository;
import com.cryptocraft.orderservice.services.OrderService;
import com.cryptocraft.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // same limit as one request line, applied after lines of the same item are merged
    private static final int MAX_QUANTITY_PER_ITEM = 10;

    private final SessionParticipantRepository sessionParticipantRepository;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PricingClient pricingClient;
    private final OrderMapper orderMapper;
    // stock write-off and Kafka OrderPlaced happen after commit (OrderPlacedListener)
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Result placeOrder(UUID participantId, PlaceOrderRequestDto dto) {
        SessionParticipant participant = sessionParticipantRepository.findByIdWithSession(participantId)
                .orElseThrow(() -> new EntityNotFoundException("Participant not found"));

        Optional<Order> existing = orderRepository.findByClientOrderIdWithLines(dto.clientOrderId());
        if (existing.isPresent()) {
            if (!existing.get().getParticipant().getParticipantId().equals(participantId)) {
                throw new OrderRejectedException("clientOrderId is already used by another guest");
            }
            log.info("Order with clientOrderId:{} was already placed", dto.clientOrderId());
            return new Result(orderMapper.toDto(existing.get()), false);
        }

        TableSession session = participant.getSession();
        if (session.getStatus() != SessionStatus.OPEN) {
            throw new OrderRejectedException("The visit at this table is closed");
        }

        Map<Long, Integer> quantityByItem = mergeLines(dto);
        Map<Long, SellableItemDto> menu = loadMenu(quantityByItem);
        Map<Long, BigDecimal> prices = lockPrices(quantityByItem, menu);

        Order order = Order.builder()
                .session(session)
                .participant(participant)
                .clientOrderId(dto.clientOrderId())
                .status(OrderStatus.PLACED)
                .stockStatus(StockStatus.PENDING)
                .total(BigDecimal.ZERO)
                .build();
        quantityByItem.forEach((itemId, quantity) -> {
            SellableItemDto item = menu.get(itemId);
            BigDecimal unitPrice = prices.get(itemId);
            order.addLine(OrderLine.builder()
                    .itemId(itemId)
                    .itemName(item.name())
                    .itemType(ItemType.valueOf(item.type()))
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .lineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                    .build());
        });
        order.setTotal(order.getLines().stream()
                .map(OrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderPlacedEvent(saved.getOrderId()));
        log.info("Order:{} placed by participant:{} at table:{}, total:{}",
                saved.getOrderId(), participantId, session.getTable().getLabel(), saved.getTotal());
        return new Result(orderMapper.toDto(saved), true);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(UUID orderId) {
        return orderRepository.findByIdWithLines(orderId)
                .map(orderMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(format("Order with id:%s, not founded", orderId)));
    }

    // the same item listed twice is one line with the summed quantity
    private static Map<Long, Integer> mergeLines(PlaceOrderRequestDto dto) {
        Map<Long, Integer> quantityByItem = dto.lines().stream()
                .collect(Collectors.toMap(
                        PlaceOrderRequestDto.Line::itemId,
                        PlaceOrderRequestDto.Line::quantity,
                        Integer::sum,
                        LinkedHashMap::new));
        quantityByItem.forEach((itemId, quantity) -> {
            if (quantity > MAX_QUANTITY_PER_ITEM) {
                throw new IllegalArgumentException(
                        format("quantity of item %d must be at most %d", itemId, MAX_QUANTITY_PER_ITEM));
            }
        });
        return quantityByItem;
    }

    // the venue's menu from Inventory: every item must exist and not be on the stop-list
    private Map<Long, SellableItemDto> loadMenu(Map<Long, Integer> quantityByItem) {
        Map<Long, SellableItemDto> menu = inventoryClient.getSellableItems(TenantContext.getRequired()).stream()
                .collect(Collectors.toMap(SellableItemDto::itemId, Function.identity()));

        List<Long> missing = quantityByItem.keySet().stream().filter(id -> !menu.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new EntityNotFoundException("Items not found: " + missing);
        }
        List<String> stopped = quantityByItem.keySet().stream()
                .map(menu::get)
                .filter(item -> !item.available())
                .map(SellableItemDto::name)
                .toList();
        if (!stopped.isEmpty()) {
            throw new OrderRejectedException("Not available right now: " + String.join(", ", stopped));
        }
        return menu;
    }

    // the price each item has at this moment; it never changes for this order
    private Map<Long, BigDecimal> lockPrices(Map<Long, Integer> quantityByItem, Map<Long, SellableItemDto> menu) {
        Long tenantId = TenantContext.getRequired();
        Map<Long, BigDecimal> prices = pricingClient.getCurrentPrices(quantityByItem.keySet()).stream()
                .filter(price -> tenantId.equals(price.tenantId()) && !price.stopped())
                .collect(Collectors.toMap(
                        CurrentPriceDto::itemId,
                        price -> price.price().setScale(2, RoundingMode.HALF_UP)));

        List<String> unpriced = quantityByItem.keySet().stream()
                .filter(id -> !prices.containsKey(id))
                .map(id -> menu.get(id).name())
                .toList();
        if (!unpriced.isEmpty()) {
            throw new OrderRejectedException("No current price for: " + String.join(", ", unpriced));
        }
        return prices;
    }
}
