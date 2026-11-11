package com.cryptocraft.inventoryservice.controllers.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * {@code hasShortages} is true when recorded stock did not cover the recipes; the order still goes
 * ahead (stock data is often behind reality), staff should check those products.
 */
public record StockConsumptionResponseDto(
        String orderId,
        Instant createdAt,
        boolean hasShortages,
        List<Line> lines
) {
    public StockConsumptionResponseDto {
        lines = (lines == null) ? List.of() : List.copyOf(lines);
    }

    public record Line(
            Long productId,
            String productName,
            BigDecimal requested,
            BigDecimal consumed,
            BigDecimal shortage
    ) {}
}
