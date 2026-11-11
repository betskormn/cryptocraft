package com.cryptocraft.orderservice.clients.dto;

import java.util.List;

// Inventory POST /api/v1/stock-consumptions; idempotent by orderId
public record StockConsumptionRequestDto(
        String orderId,
        List<Line> lines
) {
    public record Line(
            Long itemId,
            Integer quantity
    ) {}
}
