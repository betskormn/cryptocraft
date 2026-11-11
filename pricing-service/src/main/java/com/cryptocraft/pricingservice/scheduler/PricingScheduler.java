package com.cryptocraft.pricingservice.scheduler;

import com.cryptocraft.pricingservice.pricing.cache.PriceConfigCache;
import com.cryptocraft.pricingservice.pricing.engine.PricingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@RequiredArgsConstructor
public class PricingScheduler {

    private final PricingEngine pricingEngine;
    private final PriceConfigCache cache;

    private int currentGroup = 0;

    @Scheduled(fixedRate = 100)
//    @Scheduled(fixedRate = 1000)
    public void tick() {
        System.out.println("Running group: " + currentGroup);
        List<Long> items = cache.getGroup(currentGroup);
        if (!items.isEmpty()) {
            pricingEngine.process(items);
        }
        currentGroup = (currentGroup + 1) % 30;
    }
}

