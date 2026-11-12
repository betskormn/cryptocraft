package com.cryptocraft.pricingservice.pricing.scheduler;

import com.cryptocraft.pricingservice.client.InventoryClient;
import com.cryptocraft.pricingservice.pricing.cache.PriceConfigCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refreshes price configuration from Inventory. Checks every few seconds but only calls Inventory
 * when the configuration is missing (startup, Inventory was down) or older than the refresh interval.
 * A failed call keeps the previous configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceConfigScheduler {

    private final InventoryClient inventoryClient;
    private final PriceConfigCache cache;

    @Value("${pricing.config-refresh-seconds:60}")
    private long refreshSeconds;

    private volatile long lastRefreshMillis = 0;

    @Scheduled(fixedDelayString = "${pricing.config-retry-ms:5000}")
    public void refresh() {
        long now = System.currentTimeMillis();
        if (!cache.isEmpty() && now - lastRefreshMillis < refreshSeconds * 1000) {
            return;
        }
        inventoryClient.getAllPrices().ifPresent(prices -> {
            cache.update(prices, now);
            lastRefreshMillis = now;
            log.info("Price configuration refreshed: {} items", prices.size());
        });
    }
}
