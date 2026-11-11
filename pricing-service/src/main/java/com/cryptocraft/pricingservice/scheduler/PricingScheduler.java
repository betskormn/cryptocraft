package com.cryptocraft.pricingservice.scheduler;

import com.cryptocraft.pricingservice.pricing.cache.PriceConfigCache;
import com.cryptocraft.pricingservice.pricing.engine.PricingEngine;
import com.cryptocraft.pricingservice.pricing.model.PriceConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Moves each item's price once per its venue's price tick. A new item gets a price right away and is
 * then shifted by a random offset, so a venue's prices change one by one rather than all at once.
 * Items on the stop-list keep their price: it is announced once (screens show them as sold out) and
 * frozen until the item is back on sale.
 */
@Component
@RequiredArgsConstructor
public class PricingScheduler {

    // marks a stopped item that was already announced
    private static final long FROZEN = Long.MAX_VALUE;

    private final PricingEngine pricingEngine;
    private final PriceConfigCache cache;

    // itemId -> epoch millis of the next price change; only touched from the scheduler thread
    private final Map<Long, Long> nextChangeAt = new HashMap<>();

    @Scheduled(fixedRateString = "${pricing.scheduler-tick-ms:100}")
    public void tick() {
        long now = System.currentTimeMillis();
        Set<Long> activeItems = cache.getAll().stream().map(PriceConfig::itemId).collect(Collectors.toSet());
        nextChangeAt.keySet().retainAll(activeItems);

        for (PriceConfig config : cache.getAll()) {
            long tickMillis = config.tickSeconds() * 1000L;
            Long due = nextChangeAt.get(config.itemId());

            if (config.stopped()) {
                if (due == null || due != FROZEN) {
                    pricingEngine.publishCurrent(config);
                    nextChangeAt.put(config.itemId(), FROZEN);
                }
            } else if (due == null || due == FROZEN) {
                // new, or just back on sale: move now, then spread over the venue's tick
                pricingEngine.process(config);
                nextChangeAt.put(config.itemId(), now + ThreadLocalRandom.current().nextLong(1, tickMillis + 1));
            } else if (now >= due) {
                pricingEngine.process(config);
                nextChangeAt.put(config.itemId(), now + tickMillis);
            }
        }
    }
}
