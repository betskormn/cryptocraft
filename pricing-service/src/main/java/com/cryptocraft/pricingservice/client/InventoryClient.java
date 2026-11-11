package com.cryptocraft.pricingservice.client;

import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    public List<ItemPriceDto> getAllPrices() {
        try {
            return webClientBuilder
                    .build()
                    .get()
                    .uri("http://inventory-service:8081/internal/prices")
                    .retrieve()
                    .bodyToFlux(ItemPriceDto.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Inventory call failed", e);
            return List.of();
        }
    }

}
