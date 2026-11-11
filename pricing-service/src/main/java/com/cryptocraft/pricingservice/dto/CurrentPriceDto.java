package com.cryptocraft.pricingservice.dto;

import java.math.BigDecimal;

public record CurrentPriceDto(
        Long itemId,
        Long tenantId,
        BigDecimal price,
        boolean stopped
) {}
