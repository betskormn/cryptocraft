package com.cryptocraft.inventoryservice;

import com.cryptocraft.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Venue isolation end to end: the HTTP tenant filter, PostgreSQL row-level security under the
 * application role, the platform API and the cross-tenant internal endpoints.
 */
class TenantIsolationTest extends AbstractInventoryIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void requestsWithoutAValidTenantAreRejected() {
        assertThat(call(HttpMethod.GET, "/api/v1/brands", null, null).status()).isEqualTo(400);
        assertThat(call(HttpMethod.GET, "/api/v1/brands", "abc", null).status()).isEqualTo(400);
        assertThat(call(HttpMethod.GET, "/api/v1/brands", "999999", null).status()).isEqualTo(404);
    }

    @Test
    void venueCannotSeeOrChangeAnotherVenuesData() {
        long venueA = createVenue(false);
        long venueB = createVenue(false);

        Response created = call(HttpMethod.POST, "/api/v1/brands", venueA, Map.of("name", "Only in A"));
        assertThat(created.status()).isEqualTo(201);
        long brandId = created.json().get("brandId").asLong();

        assertThat(names(call(HttpMethod.GET, "/api/v1/brands", venueB, null))).doesNotContain("Only in A");
        assertThat(call(HttpMethod.GET, "/api/v1/brands/" + brandId, venueB, null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.DELETE, "/api/v1/brands/" + brandId, venueB, null).status()).isEqualTo(404);

        assertThat(call(HttpMethod.GET, "/api/v1/brands/" + brandId, venueA, null).status()).isEqualTo(200);
        assertThat(names(call(HttpMethod.GET, "/api/v1/brands", venueA, null))).containsExactly("Only in A");
    }

    @Test
    void venuesMayUseTheSameNames() {
        long venueA = createVenue(false);
        long venueB = createVenue(false);

        assertThat(call(HttpMethod.POST, "/api/v1/brands", venueA, Map.of("name", "Shared name")).status()).isEqualTo(201);
        assertThat(call(HttpMethod.POST, "/api/v1/brands", venueB, Map.of("name", "Shared name")).status()).isEqualTo(201);
    }

    @Test
    void demoMenuCopiesTheTemplateCatalog() {
        long templateId = call(HttpMethod.GET, "/api/v1/platform/tenants/by-slug/demo-template", null, null)
                .json().get("tenantId").asLong();
        List<Long> templateItems = ids(call(HttpMethod.GET, "/api/v1/items", templateId, null), "itemId");
        assertThat(templateItems).isNotEmpty();

        long venue = createVenue(true);

        List<Long> venueItems = ids(call(HttpMethod.GET, "/api/v1/items", venue, null), "itemId");
        assertThat(venueItems).hasSameSizeAs(templateItems).doesNotContainAnyElementsOf(templateItems);
        assertThat(call(HttpMethod.GET, "/api/v1/brands", venue, null).json().size())
                .isEqualTo(call(HttpMethod.GET, "/api/v1/brands", templateId, null).json().size());
    }

    @Test
    void duplicateSlugIsAConflict() {
        String slug = uniqueSlug();
        assertThat(call(HttpMethod.POST, "/api/v1/platform/tenants", null, Map.of("name", slug, "slug", slug)).status())
                .isEqualTo(201);
        assertThat(call(HttpMethod.POST, "/api/v1/platform/tenants", null, Map.of("name", slug + " 2", "slug", slug)).status())
                .isEqualTo(409);
    }

    @Test
    void suspendedVenueIsForbiddenAndGetsNoPrices() {
        long venue = createVenue(true);
        assertThat(priceTenantIds()).contains(venue);

        suspend(venue);

        assertThat(call(HttpMethod.GET, "/api/v1/items", venue, null).status()).isEqualTo(403);
        assertThat(priceTenantIds()).doesNotContain(venue);
    }

    @Test
    void internalPricesSkipTheTemplate() {
        long templateId = call(HttpMethod.GET, "/api/v1/platform/tenants/by-slug/demo-template", null, null)
                .json().get("tenantId").asLong();
        assertThat(priceTenantIds()).isNotEmpty().doesNotContain(templateId);
    }

    @Test
    void applicationRoleSeesNoRowsWithoutTenant() throws Exception {
        long venue = createVenue(true);
        JdbcClient jdbc = JdbcClient.create(dataSource);

        // the data exists: the migration user (superuser) sees it
        try (Connection superuser = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             ResultSet rs = superuser.createStatement().executeQuery("SELECT count(*) FROM items")) {
            rs.next();
            assertThat(rs.getLong(1)).isPositive();
        }

        assertThat(jdbc.sql("SELECT count(*) FROM items").query(Long.class).single()).isZero();

        TenantContext.set(venue);
        assertThat(jdbc.sql("SELECT count(DISTINCT tenant_id) FROM items").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void applicationRoleCannotWriteIntoAnotherVenue() {
        long venueA = createVenue(false);
        long venueB = createVenue(false);
        JdbcClient jdbc = JdbcClient.create(dataSource);

        TenantContext.set(venueA);
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO brands (name, tenant_id) VALUES ('sneaky', ?)")
                .param(venueB)
                .update())
                .rootCause()
                .hasMessageContaining("row-level security");
    }

    // --- helpers ---

    private void suspend(long venue) {
        Response response = call(HttpMethod.PUT, "/api/v1/platform/tenants/" + venue, null, Map.of(
                "name", "suspended-" + venue,
                "currency", "UAH",
                "timezone", "Europe/Kyiv",
                "priceTickSeconds", 30,
                "status", "SUSPENDED"));
        assertThat(response.status()).isEqualTo(200);
    }

    private List<Long> priceTenantIds() {
        return ids(call(HttpMethod.GET, "/internal/prices", null, null), "tenantId").stream().distinct().toList();
    }

    private static List<String> names(Response response) {
        return StreamSupport.stream(response.json().spliterator(), false)
                .map(node -> node.get("name").asString())
                .toList();
    }

    private static List<Long> ids(Response response, String field) {
        assertThat(response.status()).isEqualTo(200);
        return StreamSupport.stream(response.json().spliterator(), false)
                .map(node -> node.get(field).asLong())
                .toList();
    }
}
