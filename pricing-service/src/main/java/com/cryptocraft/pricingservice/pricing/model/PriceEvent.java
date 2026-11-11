package com.cryptocraft.pricingservice.pricing.model;

import java.math.BigDecimal;

/**
 * Published to Kafka; carries the venue and item details (incl. menu position and stop-list state)
 * so screens need no separate menu lookup.
 */
public record PriceEvent(
        Long tenantId,
        String tenantSlug,
        Long itemId,
        String name,
        String type,
        Integer displayOrder,
        boolean stopped,
        BigDecimal price,
        long timestamp,
        long version
) {}
