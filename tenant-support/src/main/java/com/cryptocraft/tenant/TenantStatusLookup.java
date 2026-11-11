package com.cryptocraft.tenant;

import java.util.Optional;

/**
 * Where a service learns whether a venue exists and is usable: Inventory reads its tenants table,
 * other services ask Inventory. Empty means the venue does not exist.
 */
public interface TenantStatusLookup {

    Optional<TenantStatus> findStatus(Long tenantId);
}
