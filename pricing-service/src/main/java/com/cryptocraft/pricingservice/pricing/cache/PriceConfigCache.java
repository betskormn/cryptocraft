package com.cryptocraft.pricingservice.pricing.cache;

import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import com.cryptocraft.pricingservice.pricing.model.PriceConfig;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PriceConfigCache {

    // replaced as a whole, so readers never see a half-updated configuration
    private volatile Map<Long, PriceConfig> configs = Map.of();

    // full refresh from Inventory
    public synchronized void update(Collection<ItemPriceDto> prices) {
        configs = prices.stream()
                .map(PriceConfigCache::toConfig)
                .collect(Collectors.toUnmodifiableMap(PriceConfig::itemId, Function.identity()));
    }

    // single item from an Inventory event; copy-on-write, readers keep a consistent snapshot
    public synchronized PriceConfig upsert(ItemPriceDto price) {
        PriceConfig config = toConfig(price);
        Map<Long, PriceConfig> next = new HashMap<>(configs);
        next.put(config.itemId(), config);
        configs = Map.copyOf(next);
        return config;
    }

    public synchronized void remove(Long itemId) {
        Map<Long, PriceConfig> next = new HashMap<>(configs);
        next.remove(itemId);
        configs = Map.copyOf(next);
    }

    private static PriceConfig toConfig(ItemPriceDto p) {
        return new PriceConfig(
                p.itemId(),
                p.tenantId(),
                p.tenantSlug(),
                p.priceTickSeconds(),
                p.name(),
                p.type(),
                p.displayOrder(),
                p.stopped(),
                p.basePrice(),
                p.minPrice(),
                p.maxPrice());
    }

    public Optional<PriceConfig> get(Long itemId) {
        return Optional.ofNullable(configs.get(itemId));
    }

    public Collection<PriceConfig> getAll() {
        return configs.values();
    }

    public boolean isEmpty() {
        return configs.isEmpty();
    }
}
