package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.clients.InventoryClient;
import com.cryptocraft.orderservice.clients.dto.StockConsumptionRequestDto;
import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.dao.enums.StockStatus;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import com.cryptocraft.orderservice.repositories.OrderRepository;
import com.cryptocraft.orderservice.services.StockWriteOffService;
import com.cryptocraft.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockWriteOffServiceImpl implements StockWriteOffService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    // own transaction: runs after the order's transaction committed, or from the retry job
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean writeOff(UUID orderId) {
        Order order = orderRepository.findByIdWithLines(orderId).orElse(null);
        if (order == null || order.getStockStatus() != StockStatus.PENDING) {
            return true;
        }
        StockConsumptionRequestDto request = new StockConsumptionRequestDto(
                orderId.toString(),
                order.getLines().stream()
                        .map(line -> new StockConsumptionRequestDto.Line(line.getItemId(), line.getQuantity()))
                        .toList());
        try {
            inventoryClient.writeOffStock(TenantContext.getRequired(), request);
        } catch (DownstreamUnavailableException e) {
            log.warn("Stock write-off for order:{} postponed: {}", orderId,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return false;
        }
        order.setStockStatus(StockStatus.WRITTEN_OFF);
        orderRepository.save(order);
        log.info("Stock for order:{} written off", orderId);
        return true;
    }
}
