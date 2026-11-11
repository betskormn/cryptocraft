package com.cryptocraft.inventoryservice;

import com.cryptocraft.inventoryservice.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Venue isolation end to end: the HTTP tenant filter, PostgreSQL row-level security under the
 * application role, the platform API and the cross-tenant internal endpoints.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantIsolationTest {

    private static final String APP_DB_PASSWORD = "test-app-password";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:latest");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // migrations run as the container superuser, the service as the RLS-bound application role
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.parameters.app_db_password", () -> APP_DB_PASSWORD);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "cryptocraft_app");
        registry.add("spring.datasource.password", () -> APP_DB_PASSWORD);
        registry.add("eureka.client.enabled", () -> "false");
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.create("http://localhost:" + port);
    }

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

    private long createVenue(boolean demoMenu) {
        String slug = uniqueSlug();
        Response response = call(HttpMethod.POST, "/api/v1/platform/tenants", null,
                Map.of("name", slug, "slug", slug, "demoMenu", demoMenu));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("tenantId").asLong();
    }

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

    private static String uniqueSlug() {
        return "venue-" + UUID.randomUUID().toString().substring(0, 8);
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

    private Response call(HttpMethod method, String path, Object tenantId, Object body) {
        RestClient.RequestBodySpec request = http.method(method).uri(path);
        if (tenantId != null) {
            request.header("X-Tenant-Id", String.valueOf(tenantId));
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(JSON.writeValueAsString(body));
        }
        return request.exchange((req, res) ->
                new Response(res.getStatusCode().value(), new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private record Response(int status, String body) {
        JsonNode json() {
            return JSON.readTree(body);
        }
    }
}
