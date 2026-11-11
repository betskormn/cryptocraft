package com.cryptocraft.inventoryservice.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Copies a venue's whole catalog (brands, products, items, recipes) into another venue through
 * the SECURITY DEFINER function clone_tenant_catalog(), because RLS hides the source venue.
 */
@Repository
@RequiredArgsConstructor
public class TenantCatalogRepository {

    private final JdbcClient jdbcClient;

    public void cloneCatalog(Long sourceTenantId, Long targetTenantId) {
        jdbcClient.sql("SELECT clone_tenant_catalog(?, ?)")
                .params(sourceTenantId, targetTenantId)
                .query()
                .listOfRows();
    }
}
