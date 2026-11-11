package com.cryptocraft.orderservice.clients.dto;

// Inventory GET /api/v1/items/sellable
public record SellableItemDto(
        Long itemId,
        String name,
        String type,
        boolean available
) {}
