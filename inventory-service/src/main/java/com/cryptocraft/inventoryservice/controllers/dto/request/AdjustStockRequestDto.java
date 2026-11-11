package com.cryptocraft.inventoryservice.controllers.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Positive delta adds a delivery, negative delta writes stock off; zero is rejected by the service.
 */
public record AdjustStockRequestDto(
        @NotNull(message = "delta must not be null")
        @Digits(integer = 9, fraction = 3, message = "delta may have at most 3 decimals")
        BigDecimal delta
) {}
