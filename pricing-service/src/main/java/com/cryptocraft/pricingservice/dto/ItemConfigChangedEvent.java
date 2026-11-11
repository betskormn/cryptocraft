package com.cryptocraft.pricingservice.dto;

/**
 * From Inventory (topic item-config-updates) after an item was created, updated, stopped or deleted.
 * {@code removed}: the item no longer gets live prices; otherwise {@code config} is its new configuration.
 */
public record ItemConfigChangedEvent(
        Long itemId,
        boolean removed,
        ItemPriceDto config
) {}
