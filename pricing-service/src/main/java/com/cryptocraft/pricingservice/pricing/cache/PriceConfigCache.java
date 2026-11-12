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

    // itemId -> when an Inventory event last changed it; a full refresh fetched before that must not undo it
    private final Map<Long, Long> eventAppliedAt = new HashMap<>();

    /**
     * Full refresh from Inventory. {@code fetchStartedAt} is when the request to Inventory was sent: items
     * changed by an event after that keep their event state, the snapshot may predate the change.
     */
    public synchronized void update(Collection<ItemPriceDto> prices, long fetchStartedAt) {
        Map<Long, PriceConfig> next = prices.stream()
                .map(PriceConfigCache::toConfig)
                .collect(Collectors.toMap(PriceConfig::itemId, Function.identity(), (a, b) -> b, HashMap::new));

        eventAppliedAt.entrySet().removeIf(e -> e.getValue() < fetchStartedAt);
        eventAppliedAt.keySet().forEach(itemId -> {
            PriceConfig fromEvent = configs.get(itemId);
            if (fromEvent != null) {
                next.put(itemId, fromEvent);
            } else {
                next.remove(itemId);
            }
        });
        configs = Map.copyOf(next);
    }

    // single item from an Inventory event; copy-on-write, readers keep a consistent snapshot
    public synchronized PriceConfig upsert(ItemPriceDto price) {
        PriceConfig config = toConfig(price);
        Map<Long, PriceConfig> next = new HashMap<>(configs);
        next.put(config.itemId(), config);
        configs = Map.copyOf(next);
        eventAppliedAt.put(config.itemId(), System.currentTimeMillis());
        return config;
    }

    public synchronized void remove(Long itemId) {
        Map<Long, PriceConfig> next = new HashMap<>(configs);
        next.remove(itemId);
        configs = Map.copyOf(next);
        eventAppliedAt.put(itemId, System.currentTimeMillis());
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
