package com.cryptocraft.inventoryservice.controllers.dto.response;

import com.cryptocraft.inventoryservice.dao.enums.Unit;

import java.math.BigDecimal;

public record ProductResponseDto(
        Long productId,
        String name,
        String description,
        String image,
        BigDecimal quantityInStock,
        Unit unit,
        Long brandId
) {
}
