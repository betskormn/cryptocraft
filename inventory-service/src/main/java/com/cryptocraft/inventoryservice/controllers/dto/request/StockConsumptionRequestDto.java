package com.cryptocraft.inventoryservice.controllers.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Items of an order sent to the kitchen/bar; their recipes are written off from stock.
 * {@code orderId} is the caller's id and makes the request idempotent.
 */
public record StockConsumptionRequestDto(
        @NotBlank(message = "orderId must not be blank")
        @Size(max = 64, message = "orderId must be at most 64 characters")
        String orderId,

        @NotEmpty(message = "Lines must not be empty")
        List<@Valid Line> lines
) {
    public StockConsumptionRequestDto {
        lines = (lines == null) ? List.of() : List.copyOf(lines);
    }

    public record Line(
            @NotNull(message = "itemId must not be null")
            Long itemId,

            @NotNull(message = "quantity must not be null")
            @Positive(message = "quantity must be > 0")
            Integer quantity
    ) {}
}
