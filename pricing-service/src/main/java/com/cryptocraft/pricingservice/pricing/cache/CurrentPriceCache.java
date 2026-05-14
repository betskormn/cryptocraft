package com.cryptocraft.pricingservice.pricing.cache;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CurrentPriceCache {

    private final Map<Long, BigDecimal> cache = new ConcurrentHashMap<>();

    public BigDecimal get(Long id) {
        return cache.get(id);
    }

    public void put(Long id, BigDecimal price) {
        cache.put(id, price);
    }
}

