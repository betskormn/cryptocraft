package com.cryptocraft.pricingservice.controllers;

import com.cryptocraft.pricingservice.dto.CurrentPriceDto;
import com.cryptocraft.pricingservice.pricing.cache.PriceConfigCache;
import com.cryptocraft.pricingservice.pricing.engine.PricingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

// service-to-service (Order Service); not routed through the API gateway
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/internal/current-prices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Current prices (Internal)", description = "Prices at this moment, for locking an order's prices")
public class CurrentPriceController {

    private final PriceConfigCache cache;
    private final PricingEngine pricingEngine;

    @Operation(summary = "Get current prices",
            description = "Current price of each requested item; items Pricing does not know (deleted, venue not active) are left out")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current prices of the known items")
    })
    @GetMapping
    public ResponseEntity<List<CurrentPriceDto>> getCurrentPrices(
            @Parameter(description = "Item ids", example = "1,2,3")
            @RequestParam List<Long> itemIds
    ) {
        List<CurrentPriceDto> prices = itemIds.stream()
                .distinct()
                .map(cache::get)
                .flatMap(Optional::stream)
                .map(config -> new CurrentPriceDto(config.itemId(), config.tenantId(), pricingEngine.quote(config), config.stopped()))
                .toList();
        return ResponseEntity.ok(prices);
    }
}
