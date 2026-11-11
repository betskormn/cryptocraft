package com.cryptocraft.pricingservice.pricing.model;

import java.math.BigDecimal;

public record PriceConfig(
        Long itemId,
        Long tenantId,
        String tenantSlug,
        int tickSeconds,
        String name,
        String type,
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}
