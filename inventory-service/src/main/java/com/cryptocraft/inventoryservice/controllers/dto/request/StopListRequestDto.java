package com.cryptocraft.inventoryservice.controllers.dto.request;

import jakarta.validation.constraints.NotNull;

public record StopListRequestDto(
        @NotNull(message = "stopped must not be null")
        Boolean stopped
) {}
