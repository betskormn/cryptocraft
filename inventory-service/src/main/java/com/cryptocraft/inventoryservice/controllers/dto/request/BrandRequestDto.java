package com.cryptocraft.inventoryservice.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequestDto(
        @NotBlank(message = "Brand name must not be blank")
        @Size(max = 120, message = "Brand name must be at most 120 characters")
        String name
) {}
