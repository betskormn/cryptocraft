package com.cryptocraft.pricingservice.pricing.engine;

import com.cryptocraft.pricingservice.kafka.PriceProducer;
import com.cryptocraft.pricingservice.pricing.cache.CurrentPriceCache;
import com.cryptocraft.pricingservice.pricing.model.PriceConfig;
import com.cryptocraft.pricingservice.pricing.model.PriceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PricingEngine {

    private final CurrentPriceCache currentCache;
    private final PriceProducer producer;

    private final Random random = new Random();
    private final Map<Long, Long> versions = new ConcurrentHashMap<>();

    // moves the price one step: random walk with a pull back to the base price, kept within [min, max]
    public void process(PriceConfig config) {
        BigDecimal current = currentPrice(config);

        double delta = (random.nextDouble() - 0.5) * 0.2;
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(delta));
        BigDecimal deviation = current.subtract(config.basePrice());
        BigDecimal correction = deviation.multiply(BigDecimal.valueOf(-0.05));
        BigDecimal newPrice = current
                .multiply(factor)
                .add(correction);

        publish(config, clamp(newPrice, config));
    }

    /**
     * Announces the current price again without moving it: for items on the stop-list (their price is
     * frozen) and after an item change, so screens pick up the new state right away.
     */
    public void publishCurrent(PriceConfig config) {
        publish(config, clamp(currentPrice(config), config));
    }

    // the price a guest pays right now: last published price, or the base price before the first move
    public BigDecimal quote(PriceConfig config) {
        return clamp(currentPrice(config), config);
    }

    private BigDecimal currentPrice(PriceConfig config) {
        BigDecimal current = currentCache.get(config.itemId());
        return current != null ? current : config.basePrice();
    }

    // also re-applies the range when min/max were changed in the admin
    private static BigDecimal clamp(BigDecimal price, PriceConfig config) {
        return price
                .max(config.minPrice())
                .min(config.maxPrice())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void publish(PriceConfig config, BigDecimal price) {
        Long itemId = config.itemId();
        currentCache.put(itemId, price);

        long version = versions.merge(itemId, 1L, Long::sum);
        producer.sendPrice(
                new PriceEvent(
                        config.tenantId(),
                        config.tenantSlug(),
                        itemId,
                        config.name(),
                        config.type(),
                        config.displayOrder(),
                        config.stopped(),
                        price,
                        System.currentTimeMillis(),
                        version
                )
        );
        log.debug("KAFKA SEND → tenant={} itemId={} | price={} | stopped={} | version={}",
                config.tenantSlug(), itemId, price, config.stopped(), version);
    }
}
