package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.OrderStatus;
import com.cryptocraft.orderservice.dao.enums.StockStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        UUID orderId,
        String clientOrderId,
        UUID participantId,
        OrderStatus status,
        StockStatus stockStatus,
        BigDecimal total,
        Instant createdAt,
        List<OrderLineResponseDto> lines
) {
    public OrderResponseDto {
        lines = (lines == null) ? List.of() : List.copyOf(lines);
    }
}
