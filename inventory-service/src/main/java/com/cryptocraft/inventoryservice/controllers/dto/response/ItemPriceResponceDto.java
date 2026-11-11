package com.cryptocraft.inventoryservice.controllers.dto.response;

import com.cryptocraft.inventoryservice.dao.enums.Type;

import java.math.BigDecimal;

public record ItemPriceResponceDto(
        Long itemId,
        Long tenantId,
        String tenantSlug,
        Integer priceTickSeconds,
        String name,
        Type type,
        Integer displayOrder,
        boolean stopped,
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}
