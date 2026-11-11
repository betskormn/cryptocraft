package com.cryptocraft.pricingservice.client;

import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    /**
     * Price configuration of all active venues; empty when Inventory is unreachable, so the caller
     * can keep the configuration it already has instead of wiping it.
     */
    public Optional<List<ItemPriceDto>> getAllPrices() {
        try {
            // load-balanced builder: the host is the Eureka service id
            return Optional.ofNullable(webClientBuilder
                    .build()
                    .get()
                    .uri("http://inventory-service/internal/prices")
                    .retrieve()
                    .bodyToFlux(ItemPriceDto.class)
                    .collectList()
                    .block());
        } catch (Exception e) {
            log.warn("Inventory call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

}
