package com.cryptocraft.orderservice.clients.dto;

import java.math.BigDecimal;

// Pricing GET /internal/current-prices
public record CurrentPriceDto(
        Long itemId,
        Long tenantId,
        BigDecimal price,
        boolean stopped
) {}
