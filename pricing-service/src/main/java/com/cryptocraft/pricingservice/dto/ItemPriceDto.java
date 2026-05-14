package com.cryptocraft.pricingservice.dto;

import java.math.BigDecimal;

public record ItemPriceDto(
        Long itemId,
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}

