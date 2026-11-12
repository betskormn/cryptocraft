package com.cryptocraft.orderservice.clients.dto;

// Inventory GET /api/v1/platform/tenants/{id}
public record TenantDto(
        Long tenantId,
        String status,
        String name,
        String slug,
        String currency
) {
    public TenantDto(Long tenantId, String status) {
        this(tenantId, status, null, null, null);
    }
}
