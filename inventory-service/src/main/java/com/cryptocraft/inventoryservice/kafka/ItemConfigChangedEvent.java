package com.cryptocraft.inventoryservice.kafka;

import com.cryptocraft.inventoryservice.controllers.dto.response.ItemPriceResponceDto;

/**
 * Kafka message on topic item-config-updates. {@code removed} is true when the item was deleted or its
 * venue no longer gets live prices (suspended, template); otherwise {@code config} is the item's current
 * price configuration, the same shape as GET /internal/prices.
 */
public record ItemConfigChangedEvent(
        Long itemId,
        boolean removed,
        ItemPriceResponceDto config
) {}
