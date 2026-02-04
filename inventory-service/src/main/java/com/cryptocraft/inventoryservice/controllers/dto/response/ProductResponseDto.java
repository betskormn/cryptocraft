package com.cryptocraft.inventoryservice.controllers.dto.response;

import com.cryptocraft.inventoryservice.dao.enums.Unit;

public record ProductResponseDto(
        Long productId,
        String name,
        String description,
        String image,
        int quantityInStock,
        Unit unit,
        Long brandId
) {
}
