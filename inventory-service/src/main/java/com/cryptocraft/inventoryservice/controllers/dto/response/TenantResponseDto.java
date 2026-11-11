package com.cryptocraft.inventoryservice.controllers.dto.response;

import com.cryptocraft.tenant.TenantStatus;

public record TenantResponseDto(
        Long tenantId,
        String name,
        String slug,
        String currency,
        String timezone,
        TenantStatus status,
        Integer priceTickSeconds
) {}
