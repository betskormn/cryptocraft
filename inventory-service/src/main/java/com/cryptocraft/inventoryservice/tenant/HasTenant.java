package com.cryptocraft.inventoryservice.tenant;

public interface HasTenant {

    Long getTenantId();

    void setTenantId(Long tenantId);
}