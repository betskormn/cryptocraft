package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.controllers.dto.request.LineStatusRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.KitchenLineResponseDto;
import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.dao.OrderLine;
import com.cryptocraft.orderservice.dao.enums.ItemType;
import com.cryptocraft.orderservice.dao.enums.LineStatus;
import com.cryptocraft.orderservice.dao.enums.OrderStatus;
import com.cryptocraft.orderservice.exceptions.OrderRejectedException;
import com.cryptocraft.orderservice.kafka.OrderLineStatusChangedMessage;
import com.cryptocraft.orderservice.mappers.KitchenMapper;
import com.cryptocraft.orderservice.repositories.OrderLineRepository;
import com.cryptocraft.orderservice.repositories.OrderRepository;
import com.cryptocraft.orderservice.services.KitchenService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class KitchenServiceImpl implements KitchenService {

    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;
    private final KitchenMapper kitchenMapper;
    // the other screens learn about the change after commit (LineStatusChangedListener)
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<KitchenLineResponseDto> getQueue(ItemType itemType) {
        return orderLineRepository.findQueue(itemType, LineStatus.IN_WORK).stream()
                .map(kitchenMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public KitchenLineResponseDto changeStatus(Long lineId, LineStatusRequestDto dto) {
        // the order is locked first: its total changes when a line is cancelled
        UUID orderId = orderLineRepository.findOrderIdByLineId(lineId).orElseThrow(() -> lineNotFound(lineId));
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> lineNotFound(lineId));
        OrderLine line = orderLineRepository.findByIdForKitchen(lineId).orElseThrow(() -> lineNotFound(lineId));

        // not on the screens yet: the guest's first order waits for a waiter
        if (order.getStatus() == OrderStatus.AWAITING_APPROVAL) {
            throw new OrderRejectedException("The order waits until a waiter confirms the guest");
        }
        // the same tap from two screens, or a resent request
        if (line.getStatus() == dto.status()) {
            return kitchenMapper.toDto(line);
        }
        if (line.getStatus().isFinal()) {
            throw new OrderRejectedException(format("%s is already %s", line.getItemName(), line.getStatus()));
        }

        LineStatus previous = line.getStatus();
        line.setStatus(dto.status());
        line.setStatusChangedAt(Instant.now());
        if (dto.status() == LineStatus.CANCELLED) {
            line.setCancelReason(dto.reason() == null || dto.reason().isBlank() ? null : dto.reason().trim());
            recalculate(order);
        }

        eventPublisher.publishEvent(new OrderLineStatusChangedMessage(
                OrderLineStatusChangedMessage.TYPE,
                line.getTenantId(),
                orderId,
                lineId,
                line.getItemType().name(),
                line.getStatus().name(),
                line.getCancelReason(),
                line.getStatusChangedAt()));
        log.info("Line:{} ({}) of order:{} {} -> {}", lineId, line.getItemName(), orderId, previous, line.getStatus());
        return kitchenMapper.toDto(line);
    }

    // cancelled lines are not charged; an order with nothing left is cancelled as a whole
    private static void recalculate(Order order) {
        List<OrderLine> charged = order.getLines().stream()
                .filter(line -> line.getStatus() != LineStatus.CANCELLED)
                .toList();
        order.setTotal(charged.stream()
                .map(OrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (charged.isEmpty()) {
            order.setStatus(OrderStatus.CANCELLED);
        }
    }

    private static EntityNotFoundException lineNotFound(Long lineId) {
        return new EntityNotFoundException(format("Order line with id:%d, not founded", lineId));
    }
}
