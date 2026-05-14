package com.cryptocraft.pricingservice.pricing.model;

import java.math.BigDecimal;

public record PriceConfig(
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}
