package com.cryptocraft.orderservice.tenant;

import com.cryptocraft.orderservice.clients.InventoryClient;
import com.cryptocraft.tenant.TenantStatus;
import com.cryptocraft.tenant.TenantStatusLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Venues are owned by Inventory. Statuses are cached for a short time, so a suspension takes effect
 * here within that time; unknown venues are not cached.
 */
@Component
@RequiredArgsConstructor
public class InventoryTenantStatusLookup implements TenantStatusLookup {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final InventoryClient inventoryClient;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<TenantStatus> findStatus(Long tenantId) {
        Cached cached = cache.get(tenantId);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return Optional.of(cached.status());
        }
        Optional<TenantStatus> status = inventoryClient.getTenant(tenantId)
                .map(tenant -> TenantStatus.valueOf(tenant.status()));
        status.ifPresent(s -> cache.put(tenantId, new Cached(s, System.currentTimeMillis() + TTL.toMillis())));
        return status;
    }

    private record Cached(TenantStatus status, long expiresAt) {}
}
