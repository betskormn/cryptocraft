package com.cryptocraft.inventoryservice.tenant;

public final class TenantContext {
    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long tenantId) { TENANT.set(tenantId); }
    public static Long get() { return TENANT.get(); }

    public static Long getRequired() {
        Long id = TENANT.get();
        if (id == null) throw new IllegalStateException("No tenant in context");
        return id;
    }

    public static void clear() { TENANT.remove(); }
}