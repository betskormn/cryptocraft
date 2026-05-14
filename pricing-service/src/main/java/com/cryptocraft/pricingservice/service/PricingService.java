package com.cryptocraft.pricingservice.service;

import com.cryptocraft.pricingservice.kafka.PriceProducer;
import com.cryptocraft.pricingservice.pricing.model.PriceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PriceProducer priceProducer;

    private final Random random = new Random();
    private final AtomicLong version = new AtomicLong();

    public void processProduct(Long productId) {

        int stock = 50 + random.nextInt(100);
        int demand = 10 + random.nextInt(50);

        BigDecimal basePrice = BigDecimal.valueOf(10);

        BigDecimal demandFactor = BigDecimal.valueOf(demand)
                .multiply(BigDecimal.valueOf(0.02));

        BigDecimal supplyFactor = stock > 0
                ? BigDecimal.ONE.divide(BigDecimal.valueOf(stock), 6, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.valueOf(2);

        BigDecimal newPrice = basePrice
                .multiply(BigDecimal.ONE.add(demandFactor))
                .multiply(BigDecimal.ONE.add(supplyFactor));

        newPrice = newPrice
                .max(BigDecimal.valueOf(5))
                .min(BigDecimal.valueOf(100));

        PriceEvent event = new PriceEvent(
                productId,
                newPrice,
                System.currentTimeMillis(),
                version.incrementAndGet()
        );

        priceProducer.sendPrice(event);
    }
}