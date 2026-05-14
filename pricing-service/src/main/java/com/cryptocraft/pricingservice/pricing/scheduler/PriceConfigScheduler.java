package com.cryptocraft.pricingservice.pricing.scheduler;

import com.cryptocraft.pricingservice.client.InventoryClient;
import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import com.cryptocraft.pricingservice.pricing.cache.PriceConfigCache;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceConfigScheduler {

    private final InventoryClient inventoryClient;
    private final PriceConfigCache cache;

//    @PostConstruct
//    public void init() {
//        refresh();
//    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void refresh() {
        List<ItemPriceDto> prices = inventoryClient.getAllPrices();
        cache.update(prices);
    }
}
