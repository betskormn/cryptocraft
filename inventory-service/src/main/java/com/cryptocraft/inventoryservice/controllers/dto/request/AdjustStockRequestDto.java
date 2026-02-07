package com.cryptocraft.inventoryservice.controllers.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdjustStockRequestDto(
        @NotNull(message = "delta must not be null")
        @Positive(message = "delta must be > 0")
        Integer delta
) {}