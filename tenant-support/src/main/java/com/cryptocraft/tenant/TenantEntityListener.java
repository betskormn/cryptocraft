package com.cryptocraft.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class TenantEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof HasTenant t && t.getTenantId() == null) {
            t.setTenantId(TenantContext.getRequired());
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof HasTenant t && t.getTenantId() == null) {
            t.setTenantId(TenantContext.getRequired());
        }
    }
}