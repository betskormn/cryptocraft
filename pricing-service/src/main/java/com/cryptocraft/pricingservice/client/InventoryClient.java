package com.cryptocraft.pricingservice.client;

import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    public List<ItemPriceDto> getAllPrices() {

        return webClientBuilder
                .build()
                .get()
                .uri("http://inventory-service/internal/prices")
                .retrieve()
                .bodyToFlux(ItemPriceDto.class)
                .collectList()
                .block();
    }
}
