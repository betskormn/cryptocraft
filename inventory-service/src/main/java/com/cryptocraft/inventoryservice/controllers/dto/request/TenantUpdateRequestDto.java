package com.cryptocraft.inventoryservice.controllers.dto.request;

import com.cryptocraft.tenant.TenantStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Full update (PUT) of venue settings. The slug is not updatable: it is printed in QR codes.
 */
public record TenantUpdateRequestDto(
        @NotBlank(message = "Tenant name must not be blank")
        @Size(max = 255, message = "Tenant name must be at most 255 characters")
        String name,

        @NotNull(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be an ISO 4217 code, e.g. UAH")
        String currency,

        @NotBlank(message = "Timezone is required")
        String timezone,

        @NotNull(message = "Price tick is required")
        @Min(value = 1, message = "Price tick must be at least 1 second")
        @Max(value = 86400, message = "Price tick must be at most 86400 seconds")
        Integer priceTickSeconds,

        @NotNull(message = "Status is required")
        TenantStatus status
) {}
