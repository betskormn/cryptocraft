package com.cryptocraft.inventoryservice.controllers.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ItemResponseDto(
        Long itemId,
        String name,
        String recipe,
        String image,
        String type,
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer displayOrder,
        boolean stopped,
        List<ItemProductResponseDto> products
) {
    public ItemResponseDto {
        products = (products == null) ? List.of() : List.copyOf(products);
    }
}