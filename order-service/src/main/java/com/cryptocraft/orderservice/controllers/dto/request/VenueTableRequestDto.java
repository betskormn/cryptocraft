package com.cryptocraft.orderservice.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code active} defaults to true; an inactive table cannot be joined (e.g. under repair).
 */
public record VenueTableRequestDto(
        @NotBlank(message = "Table label must not be blank")
        @Size(max = 50, message = "Table label must be at most 50 characters")
        String label,

        Boolean active
) {}
