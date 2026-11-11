package com.cryptocraft.inventoryservice.controllers.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Optional fields fall back to UAH, Europe/Kyiv and a 30 second price tick.
 * demoMenu copies the menu of the demo template venue into the new venue.
 */
public record TenantRequestDto(
        @NotBlank(message = "Tenant name must not be blank")
        @Size(max = 255, message = "Tenant name must be at most 255 characters")
        String name,

        @NotBlank(message = "Slug must not be blank")
        @Size(min = 3, max = 64, message = "Slug must be 3-64 characters")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Slug may contain lowercase letters, digits and single dashes")
        String slug,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be an ISO 4217 code, e.g. UAH")
        String currency,

        String timezone,

        @Min(value = 1, message = "Price tick must be at least 1 second")
        @Max(value = 86400, message = "Price tick must be at most 86400 seconds")
        Integer priceTickSeconds,

        Boolean demoMenu
) {}
