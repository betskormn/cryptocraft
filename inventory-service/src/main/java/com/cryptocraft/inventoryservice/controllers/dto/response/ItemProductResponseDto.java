package com.cryptocraft.inventoryservice.controllers.dto.response;

public record ItemProductResponseDto(
        Long productId,
        String name,
        Integer quantity
) {}
