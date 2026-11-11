package com.cryptocraft.pricingservice.dto;

import java.math.BigDecimal;

public record ItemPriceDto(
        Long itemId,
        Long tenantId,
        String tenantSlug,
        Integer priceTickSeconds,
        String name,
        String type,
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}
