package com.cryptocraft.inventoryservice;

import com.cryptocraft.security.test.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may do what: tokens are checked against the venue in X-Tenant-Id and the role the token gives there.
 */
class InventorySecurityTest extends AbstractInventoryIntegrationTest {

    private long venue;

    @BeforeEach
    void createVenue() {
        venue = createVenue(false);
    }

    @Test
    void anonymousCallersMustSignIn() {
        assertThat(callAs(null, HttpMethod.GET, "/api/v1/brands", venue, null).status()).isEqualTo(401);
        assertThat(callAs("not-a-token", HttpMethod.GET, "/api/v1/brands", venue, null).status()).isEqualTo(401);
        // the guests' menu stays open
        assertThat(callAs(null, HttpMethod.GET, "/api/v1/items/sellable", venue, null).status()).isEqualTo(200);
    }

    @Test
    void floorStaffUseTheStopListButDoNotEditTheMenu() {
        long item = createItem();
        String staff = TestTokens.venue(venue, "STAFF");

        assertThat(callAs(staff, HttpMethod.GET, "/api/v1/items", venue, null).status()).isEqualTo(200);
        assertThat(callAs(staff, HttpMethod.PATCH, "/api/v1/items/" + item + "/stop-list", venue, Map.of("stopped", true)).status())
                .isEqualTo(200);
        assertThat(callAs(staff, HttpMethod.POST, "/api/v1/brands", venue, Map.of("name", "Staff brand")).status()).isEqualTo(403);
        assertThat(callAs(staff, HttpMethod.GET, "/api/v1/products", venue, null).status()).isEqualTo(403);
    }

    @Test
    void inventoryManagersEditTheMenu() {
        String manager = TestTokens.venue(venue, "INVENTORY_MANAGER");

        assertThat(callAs(manager, HttpMethod.POST, "/api/v1/brands", venue, Map.of("name", "Manager brand")).status()).isEqualTo(201);
    }

    @Test
    void aTokenOpensOnlyItsOwnVenues() {
        long otherVenue = createVenue(false);
        String admin = TestTokens.venue(venue, "ADMIN");

        assertThat(callAs(admin, HttpMethod.GET, "/api/v1/brands", otherVenue, null).status()).isEqualTo(403);
        // no venue at all is not "any venue"
        assertThat(callAs(admin, HttpMethod.GET, "/api/v1/brands", null, null).status()).isEqualTo(403);

        List<Long> mine = StreamSupport.stream(callAs(admin, HttpMethod.GET, "/api/v1/venues", null, null).json().spliterator(), false)
                .map(node -> node.get("tenantId").asLong())
                .toList();
        assertThat(mine).containsExactly(venue);
    }

    @Test
    void platformApiIsForPlatformAdminsAndOurServices() {
        String admin = TestTokens.venue(venue, "ADMIN");

        assertThat(callAs(admin, HttpMethod.GET, "/api/v1/platform/tenants", null, null).status()).isEqualTo(403);
        assertThat(callAs(admin, HttpMethod.POST, "/api/v1/platform/tenants", null,
                Map.of("name", uniqueSlug(), "slug", uniqueSlug())).status()).isEqualTo(403);
        // Order Service checks that a venue is active
        JsonNode tenant = callAs(TestTokens.service(), HttpMethod.GET, "/api/v1/platform/tenants/" + venue, null, null).json();
        assertThat(tenant.get("tenantId").asLong()).isEqualTo(venue);
    }

    private long createItem() {
        long brand = call(HttpMethod.POST, "/api/v1/brands", venue, Map.of("name", "Brand")).json().get("brandId").asLong();
        long lime = call(HttpMethod.POST, "/api/v1/products", venue, Map.of(
                "name", "Lime", "description", "test", "quantity", 10, "unit", "PIECE", "brandId", brand)).json().get("productId").asLong();
        return call(HttpMethod.POST, "/api/v1/items", venue, Map.of(
                "name", "Lemonade", "type", "DRINK", "basePrice", 100, "minPrice", 80, "maxPrice", 150,
                "products", List.of(Map.of("productId", lime, "quantity", 1)))).json().get("itemId").asLong();
    }
}
