package com.cryptocraft.pricestreamingservice.model;

import java.math.BigDecimal;

public record PriceEvent(
        Long itemId,
        BigDecimal price,
        long timestamp,
        long version
) {}