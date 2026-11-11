package com.cryptocraft.inventoryservice.controllers.dto.response;

/**
 * An item as guests and staff see it when ordering. {@code available} follows the stop-list only;
 * {@code portions} is an estimate from current stock (null for an item without a recipe) and is a
 * warning, it never blocks an order: recorded stock is often behind the real one.
 */
public record SellableItemResponseDto(
        Long itemId,
        String name,
        String type,
        String image,
        Integer displayOrder,
        boolean available,
        Integer portions
) {}
