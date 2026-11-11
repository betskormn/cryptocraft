package com.cryptocraft.orderservice.controllers.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The guest's cart at the moment of sending. Prices are not sent: they are taken from Pricing and locked
 * now. {@code clientOrderId} is generated once per cart by the client; resending it returns the first order.
 * Limits keep one guest from blocking the kitchen or the stock with an absurd order.
 */
public record PlaceOrderRequestDto(
        @NotBlank(message = "clientOrderId must not be blank")
        @Size(max = 64, message = "clientOrderId must be at most 64 characters")
        String clientOrderId,

        @NotEmpty(message = "Lines must not be empty")
        @Size(max = 30, message = "An order may have at most 30 lines")
        List<@Valid Line> lines
) {
    public PlaceOrderRequestDto {
        lines = (lines == null) ? List.of() : List.copyOf(lines);
    }

    public record Line(
            @NotNull(message = "itemId must not be null")
            Long itemId,

            @NotNull(message = "quantity must not be null")
            @Positive(message = "quantity must be > 0")
            @Max(value = 10, message = "quantity must be at most 10")
            Integer quantity
    ) {}
}
