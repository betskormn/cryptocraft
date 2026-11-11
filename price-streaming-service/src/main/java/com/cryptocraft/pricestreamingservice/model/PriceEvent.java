package com.cryptocraft.pricestreamingservice.model;

import java.math.BigDecimal;

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
