package com.cryptocraft.pricingservice.pricing.cache;

import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import com.cryptocraft.pricingservice.pricing.model.PriceConfig;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PriceConfigCache {

    // replaced as a whole, so readers never see a half-updated configuration
    private volatile Map<Long, PriceConfig> configs = Map.of();

    public void update(Collection<ItemPriceDto> prices) {
        configs = prices.stream()
                .map(p -> new PriceConfig(
                        p.itemId(),
                        p.tenantId(),
                        p.tenantSlug(),
                        p.priceTickSeconds(),
                        p.name(),
                        p.type(),
                        p.basePrice(),
                        p.minPrice(),
                        p.maxPrice()))
                .collect(Collectors.toUnmodifiableMap(PriceConfig::itemId, Function.identity()));
    }

    public Collection<PriceConfig> getAll() {
        return configs.values();
    }

    public boolean isEmpty() {
        return configs.isEmpty();
    }
}
