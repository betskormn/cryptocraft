package com.cryptocraft.inventoryservice.controllers.dto.response;

import java.util.List;

public record ItemResponseDto(
        Long itemId,
        String name,
        String recipe,
        String image,
        String type,
        List<ProductResponseDto> products
) {
    public ItemResponseDto {
        products = (products == null) ? List.of() : List.copyOf(products);
    }
}
