package com.cryptocraft.orderservice.clients.dto;

// Inventory GET /api/v1/platform/tenants/{id}
public record TenantDto(
        Long tenantId,
        String status
) {}
