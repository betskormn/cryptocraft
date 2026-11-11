package com.cryptocraft.pricingservice.kafka;

import com.cryptocraft.pricingservice.dto.ItemConfigChangedEvent;
import com.cryptocraft.pricingservice.pricing.cache.PriceConfigCache;
import com.cryptocraft.pricingservice.pricing.engine.PricingEngine;
import com.cryptocraft.pricingservice.pricing.model.PriceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Applies an item change from Inventory at once (the periodic refresh stays as a fallback) and
 * re-announces the item's current price, unchanged, so screens show the new state (stop-list, name,
 * menu position) immediately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemConfigConsumer {

    private final PriceConfigCache cache;
    private final PricingEngine pricingEngine;

    @KafkaListener(
            topics = "item-config-updates",
            containerFactory = "itemConfigListenerContainerFactory"
    )
    public void onItemConfigChanged(ItemConfigChangedEvent event) {
        if (event.removed() || event.config() == null) {
            cache.remove(event.itemId());
            log.info("Item:{} no longer priced", event.itemId());
            return;
        }
        PriceConfig config = cache.upsert(event.config());
        pricingEngine.publishCurrent(config);
        log.info("Item:{} config updated (stopped={})", config.itemId(), config.stopped());
    }
}
