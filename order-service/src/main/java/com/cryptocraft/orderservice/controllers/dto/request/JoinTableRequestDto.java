package com.cryptocraft.orderservice.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinTableRequestDto(
        @NotBlank(message = "tableCode must not be blank")
        @Size(max = 32, message = "tableCode must be at most 32 characters")
        String tableCode,

        @Size(max = 50, message = "Display name must be at most 50 characters")
        String displayName
) {}
