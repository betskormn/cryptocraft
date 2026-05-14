package com.cryptocraft.inventoryservice.controllers.dto.response;

import java.math.BigDecimal;

public record ItemPriceResponceDto(
        Long itemId,
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}
