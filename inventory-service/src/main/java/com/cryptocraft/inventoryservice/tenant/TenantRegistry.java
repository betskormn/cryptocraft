package com.cryptocraft.inventoryservice.tenant;

import com.cryptocraft.inventoryservice.dao.enums.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up a tenant's status for the request filter. Only existing tenants are cached, so a tenant
 * created after startup is picked up on its first request; status changes must call {@link #evict}.
 */
@Component
@RequiredArgsConstructor
public class TenantRegistry {

    private final JdbcClient jdbcClient;
    private final Map<Long, TenantStatus> statuses = new ConcurrentHashMap<>();

    public Optional<TenantStatus> findStatus(Long tenantId) {
        TenantStatus cached = statuses.get(tenantId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<TenantStatus> status = jdbcClient.sql("SELECT status FROM tenants WHERE tenant_id = ?")
                .param(tenantId)
                .query(Integer.class)
                .optional()
                .map(TenantStatus::byId);
        status.ifPresent(s -> statuses.put(tenantId, s));
        return status;
    }

    public void evict(Long tenantId) {
        statuses.remove(tenantId);
    }
}
