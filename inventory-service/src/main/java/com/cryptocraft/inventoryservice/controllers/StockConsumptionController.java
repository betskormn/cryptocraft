package com.cryptocraft.inventoryservice.controllers;

import com.cryptocraft.inventoryservice.controllers.dto.request.StockConsumptionRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.StockConsumptionResponseDto;
import com.cryptocraft.inventoryservice.services.StockConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

// service-to-service (Order Service); not routed through the API gateway
@RestController
@RequestMapping(value = "/api/v1/stock-consumptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Stock consumptions", description = "Recipe write-offs for orders sent to the kitchen/bar")
public class StockConsumptionController {

    private final StockConsumptionService stockConsumptionService;

    public StockConsumptionController(StockConsumptionService stockConsumptionService) {
        this.stockConsumptionService = stockConsumptionService;
    }

    @Operation(summary = "Write off stock for an order",
            description = "Subtracts the recipes of the order's items from stock. Idempotent by orderId: "
                    + "a repeated call changes nothing and returns the first result with 200. "
                    + "Stock never goes below zero; missing amounts are reported as shortages, the order is not rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock written off"),
            @ApiResponse(responseCode = "200", description = "Already written off for this orderId"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StockConsumptionResponseDto> consume(@RequestBody @Valid StockConsumptionRequestDto requestDto) {
        StockConsumptionService.Result result = stockConsumptionService.consume(requestDto);
        if (!result.created()) {
            return ResponseEntity.ok(result.consumption());
        }
        URI location = URI.create("/api/v1/stock-consumptions/" + result.consumption().orderId());
        return ResponseEntity.created(location).body(result.consumption());
    }

    @Operation(summary = "Get the write-off of an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Write-off found"),
            @ApiResponse(responseCode = "404", description = "No write-off for this orderId",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<StockConsumptionResponseDto> getByOrderId(
            @Parameter(description = "Order id given when stock was written off", example = "order-42")
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(stockConsumptionService.getByOrderId(orderId));
    }
}
