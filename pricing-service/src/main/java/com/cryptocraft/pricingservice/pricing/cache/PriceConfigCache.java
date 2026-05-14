package com.cryptocraft.pricingservice.pricing.cache;

import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import com.cryptocraft.pricingservice.pricing.model.PriceConfig;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PriceConfigCache {

    private final Map<Long, PriceConfig> cache = new ConcurrentHashMap<>();
    private final Map<Integer, List<Long>> groups = new ConcurrentHashMap<>();

    public void update(List<ItemPriceDto> prices) {
        cache.clear();

        for (ItemPriceDto p : prices) {
            cache.put(
                    p.itemId(),
                    new PriceConfig(
                            p.basePrice(),
                            p.minPrice(),
                            p.maxPrice()
                    )
            );
        }
        rebuildGroups();
    }

    private void rebuildGroups() {
        Map<Integer, List<Long>> tmp = new HashMap<>();
        List<Long> ids = new ArrayList<>(cache.keySet());
        Collections.shuffle(ids);
        int groupsCount = Math.min(30, Math.max(1, ids.size() / 2));
        int index = 0;
        for (int i = 0; i < ids.size(); i++) {
            Long itemId = ids.get(i);
            int group = index % groupsCount;
            tmp.computeIfAbsent(group, k -> new ArrayList<>())
                    .add(itemId);
            index++;
        }
        groups.clear();
        groups.putAll(tmp);
    }


    public PriceConfig get(Long itemId) {
        return cache.get(itemId);
    }

    public List<Long> getGroup(int group) {
        return groups.getOrDefault(group, List.of());
    }

    public Set<Long> getAllIds() {
        return cache.keySet();
    }
}

