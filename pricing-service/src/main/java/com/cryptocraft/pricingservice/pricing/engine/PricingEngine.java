package com.cryptocraft.pricingservice.pricing.engine;

import com.cryptocraft.pricingservice.kafka.PriceProducer;
import com.cryptocraft.pricingservice.pricing.model.PriceConfig;
import com.cryptocraft.pricingservice.pricing.cache.CurrentPriceCache;
import com.cryptocraft.pricingservice.pricing.cache.PriceConfigCache;
import com.cryptocraft.pricingservice.pricing.model.PriceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PricingEngine {

    private final PriceConfigCache configCache;
    private final CurrentPriceCache currentCache;
    private final PriceProducer producer;

    private final Random random = new Random();
    private final Map<Long, Long> versions = new ConcurrentHashMap<>();

    public void process(List<Long> itemIds) {
        for (Long itemId : itemIds) {

            PriceConfig config = configCache.get(itemId);
            if (config == null) continue;

            BigDecimal current = currentCache.get(itemId);
            if (current == null) {
                current = config.basePrice();
            }

            double delta = (random.nextDouble() - 0.5) * 0.2;
            BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(delta));
            BigDecimal deviation = current.subtract(config.basePrice());
            BigDecimal correction = deviation.multiply(BigDecimal.valueOf(-0.05));
            BigDecimal newPrice = current
                    .multiply(factor)
                    .add(correction);

            newPrice = newPrice
                    .max(config.minPrice())
                    .min(config.maxPrice());

            newPrice = newPrice.setScale(2, RoundingMode.HALF_UP);

            currentCache.put(itemId, newPrice);

            long version = versions.merge(itemId, 1L, Long::sum);
            producer.sendPrice(
                    new PriceEvent(
                            itemId,
                            newPrice,
                            System.currentTimeMillis(),
                            version
                    )
            );
            System.out.println(
                    "KAFKA SEND → itemId=" + itemId +
                    " | price=" + newPrice +
                    " | version=" + version
            );
        }
    }
}

