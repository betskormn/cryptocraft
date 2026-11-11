package com.cryptocraft.inventoryservice;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the service against a real PostgreSQL with the production role split: migrations as the
 * container superuser, the service as the RLS-bound application role. One container is shared by
 * all test classes (and their cached Spring context) for the whole run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// no application-test.yml: an explicit profile only keeps spring.profiles.default (local) from applying
@ActiveProfiles("test")
abstract class AbstractInventoryIntegrationTest {

    private static final String APP_DB_PASSWORD = "test-app-password";

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.parameters.app_db_password", () -> APP_DB_PASSWORD);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "cryptocraft_app");
        registry.add("spring.datasource.password", () -> APP_DB_PASSWORD);
        registry.add("eureka.client.enabled", () -> "false");
        // no Kafka in these tests; item change events are a best-effort notification
        registry.add("inventory.events.enabled", () -> "false");
    }

    static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    private RestClient http;

    @BeforeEach
    void setUpHttp() {
        http = RestClient.create("http://localhost:" + port);
    }

    long createVenue(boolean demoMenu) {
        String slug = uniqueSlug();
        Response response = call(HttpMethod.POST, "/api/v1/platform/tenants", null,
                Map.of("name", slug, "slug", slug, "demoMenu", demoMenu));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("tenantId").asLong();
    }

    static String uniqueSlug() {
        return "venue-" + UUID.randomUUID().toString().substring(0, 8);
    }

    Response call(HttpMethod method, String path, Object tenantId, Object body) {
        RestClient.RequestBodySpec request = http.method(method).uri(path);
        if (tenantId != null) {
            request.header("X-Tenant-Id", String.valueOf(tenantId));
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .body(body instanceof String raw ? raw : JSON.writeValueAsString(body));
        }
        return request.exchange((req, res) ->
                new Response(res.getStatusCode().value(), new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    record Response(int status, String body) {
        JsonNode json() {
            return JSON.readTree(body);
        }
    }
}
