package com.cryptocraft.identityservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sign-in through the real OAuth2 flow (authorization code with PKCE, as the gateway does it), the claims
 * that services rely on, refresh, service tokens and the staff API. Real PostgreSQL, real Spring Authorization Server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// no application-test.yml: an explicit profile only keeps spring.profiles.default (local) from applying
@ActiveProfiles("test")
class IdentityServiceIntegrationTest {

    private static final String APP_DB_PASSWORD = "test-identity-password";
    private static final String REDIRECT_URI = "http://admin.test/login/oauth2/code/identity";
    private static final String GATEWAY = "cryptocraft-gateway";
    private static final String GATEWAY_SECRET = "test-gateway-secret";
    private static final String ADMIN_EMAIL = "root@cryptocraft.test";
    private static final String ADMIN_PASSWORD = "root-password";

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.parameters.identity_db_password", () -> APP_DB_PASSWORD);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "cryptocraft_identity");
        registry.add("spring.datasource.password", () -> APP_DB_PASSWORD);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("identity.gateway-client.secret", () -> GATEWAY_SECRET);
        registry.add("identity.gateway-client.redirect-uri", () -> REDIRECT_URI);
        registry.add("identity.gateway-client.post-logout-redirect-uri", () -> "http://admin.test/");
        registry.add("identity.service-clients[0].client-id", () -> "order-service");
        registry.add("identity.service-clients[0].secret", () -> "test-order-secret");
        registry.add("identity.bootstrap-admin.email", () -> ADMIN_EMAIL);
        registry.add("identity.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    private String base;

    @BeforeEach
    void setUp() {
        base = "http://localhost:" + port;
    }

    // --- sign-in and tokens ---

    @Test
    void platformAdminSignsInAndGetsTokensServicesCanUse() {
        Tokens tokens = signIn(ADMIN_EMAIL, ADMIN_PASSWORD);

        JsonNode access = claims(tokens.access());
        assertThat(access.get("platform_admin").asBoolean()).isTrue();
        assertThat(access.get("venues").isObject()).isTrue();
        assertThat(access.get("email").asString()).isEqualTo(ADMIN_EMAIL);
        assertThat(claims(tokens.id()).get("aud").toString()).contains(GATEWAY);
    }

    @Test
    void staffTokenListsTheirVenuesAndRoles() {
        String admin = signIn(ADMIN_EMAIL, ADMIN_PASSWORD).access();
        String email = unique() + "@staff.test";
        assertThat(addStaff(admin, 5, email, "STAFF").status()).isEqualTo(201);
        assertThat(addStaff(admin, 9, email, "INVENTORY_MANAGER").status()).isEqualTo(201);

        JsonNode access = claims(signIn(email, "staff-password").access());

        assertThat(access.get("platform_admin").asBoolean()).isFalse();
        assertThat(access.get("venues").get("5").asString()).isEqualTo("STAFF");
        assertThat(access.get("venues").get("9").asString()).isEqualTo("INVENTORY_MANAGER");
    }

    @Test
    void wrongPasswordDoesNotSignIn() {
        Browser browser = new Browser();
        Response login = browser.startSignIn(pkce()).submit(ADMIN_EMAIL, "wrong-password");

        assertThat(login.status()).isEqualTo(302);
        assertThat(login.location()).endsWith("/login?error");
    }

    @Test
    void refreshPicksUpARoleChange() {
        String admin = signIn(ADMIN_EMAIL, ADMIN_PASSWORD).access();
        String email = unique() + "@staff.test";
        String userId = addStaff(admin, 5, email, "STAFF").json().get("userId").asString();
        addStaff(admin, 5, unique() + "@admin.test", "ADMIN");
        Tokens tokens = signIn(email, "staff-password");

        assertThat(call("PUT", "/api/v1/staff/" + userId, admin, 5L, Map.of("role", "ADMIN")).status()).isEqualTo(200);
        Response refreshed = tokenRequest(Map.of("grant_type", "refresh_token", "refresh_token", tokens.refresh()));

        assertThat(refreshed.status()).isEqualTo(200);
        assertThat(claims(refreshed.json().get("access_token").asString()).get("venues").get("5").asString()).isEqualTo("ADMIN");
        // the old refresh token was replaced
        assertThat(refreshed.json().get("refresh_token").asString()).isNotEqualTo(tokens.refresh());
    }

    @Test
    void servicesGetTokensWithTheInternalScopeOnly() {
        Response response = send(HttpRequest.newBuilder(URI.create(base + "/oauth2/token"))
                .header("Authorization", basic("order-service", "test-order-secret"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(Map.of("grant_type", "client_credentials", "scope", "internal")))));

        assertThat(response.status()).isEqualTo(200);
        JsonNode access = claims(response.json().get("access_token").asString());
        assertThat(access.get("scope").toString()).contains("internal");
        assertThat(access.has("venues")).isFalse();

        Response wrongSecret = send(HttpRequest.newBuilder(URI.create(base + "/oauth2/token"))
                .header("Authorization", basic("order-service", "nope"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(Map.of("grant_type", "client_credentials")))));
        assertThat(wrongSecret.status()).isEqualTo(401);
    }

    @Test
    void onlyOurServicesGetGuestTokensAndTheyGrantNoStaffAccess() {
        String service = send(HttpRequest.newBuilder(URI.create(base + "/oauth2/token"))
                .header("Authorization", basic("order-service", "test-order-secret"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(Map.of("grant_type", "client_credentials", "scope", "internal")))))
                .json().get("access_token").asString();
        UUID participant = UUID.randomUUID();
        Map<String, Object> request = Map.of("tenantId", 5, "participantId", participant, "sessionId", UUID.randomUUID());

        Response issued = call("POST", "/api/v1/guest-tokens", service, null, request);

        assertThat(issued.status()).isEqualTo(200);
        String guestToken = issued.json().get("token").asString();
        JsonNode claims = claims(guestToken);
        assertThat(claims.get("guest").asBoolean()).isTrue();
        assertThat(claims.get("tenant_id").asLong()).isEqualTo(5);
        assertThat(claims.get("participant_id").asString()).isEqualTo(participant.toString());
        assertThat(claims.has("venues")).isFalse();

        String staff = signIn(ADMIN_EMAIL, ADMIN_PASSWORD).access();
        assertThat(call("POST", "/api/v1/guest-tokens", staff, null, request).status()).isEqualTo(403);
        assertThat(call("POST", "/api/v1/guest-tokens", null, null, request).status()).isEqualTo(401);
        // a guest is nobody's staff
        assertThat(call("GET", "/api/v1/staff", guestToken, 5L, null).status()).isEqualTo(403);
    }

    // --- staff API ---

    @Test
    void onlyTheVenueAdminManagesItsStaff() {
        String platformAdmin = signIn(ADMIN_EMAIL, ADMIN_PASSWORD).access();
        String adminEmail = unique() + "@admin.test";
        String staffEmail = unique() + "@staff.test";
        addStaff(platformAdmin, 7, adminEmail, "ADMIN");
        addStaff(platformAdmin, 7, staffEmail, "STAFF");
        String venueAdmin = signIn(adminEmail, "staff-password").access();
        String staff = signIn(staffEmail, "staff-password").access();

        assertThat(call("GET", "/api/v1/staff", venueAdmin, 7L, null).json().size()).isEqualTo(2);
        assertThat(call("GET", "/api/v1/staff", staff, 7L, null).status()).isEqualTo(403);
        assertThat(call("GET", "/api/v1/staff", venueAdmin, 8L, null).status()).isEqualTo(403);
        assertThat(call("GET", "/api/v1/staff", null, 7L, null).status()).isEqualTo(401);
    }

    @Test
    void venueKeepsItsLastAdmin() {
        String platformAdmin = signIn(ADMIN_EMAIL, ADMIN_PASSWORD).access();
        String userId = addStaff(platformAdmin, 11, unique() + "@admin.test", "ADMIN").json().get("userId").asString();

        assertThat(call("PUT", "/api/v1/staff/" + userId, platformAdmin, 11L, Map.of("role", "STAFF")).status()).isEqualTo(409);
        assertThat(call("DELETE", "/api/v1/staff/" + userId, platformAdmin, 11L, null).status()).isEqualTo(409);
    }

    @Test
    void existingAccountJoinsAnotherVenueAndTwiceIsAConflict() {
        String platformAdmin = signIn(ADMIN_EMAIL, ADMIN_PASSWORD).access();
        String email = unique() + "@staff.test";
        addStaff(platformAdmin, 12, email, "STAFF");

        // no password needed: the account exists
        Response joined = call("POST", "/api/v1/staff", platformAdmin, 13L, Map.of("email", email.toUpperCase(), "role", "STAFF"));
        assertThat(joined.status()).isEqualTo(201);
        assertThat(addStaff(platformAdmin, 13, email, "ADMIN").status()).isEqualTo(409);

        Response newWithoutPassword = call("POST", "/api/v1/staff", platformAdmin, 13L,
                Map.of("email", unique() + "@new.test", "displayName", "New", "role", "STAFF"));
        assertThat(newWithoutPassword.status()).isEqualTo(400);
    }

    @Test
    void meShowsMyVenuesAndPasswordCanBeChanged() {
        String platformAdmin = signIn(ADMIN_EMAIL, ADMIN_PASSWORD).access();
        String email = unique() + "@staff.test";
        addStaff(platformAdmin, 14, email, "STAFF");
        String staff = signIn(email, "staff-password").access();

        JsonNode me = call("GET", "/api/v1/me", staff, null, null).json();
        assertThat(me.get("venues").get(0).get("tenantId").asLong()).isEqualTo(14);
        assertThat(me.get("venues").get(0).get("role").asString()).isEqualTo("STAFF");

        assertThat(call("PUT", "/api/v1/me/password", staff, null,
                Map.of("currentPassword", "wrong", "newPassword", "new-password-1")).status()).isEqualTo(409);
        assertThat(call("PUT", "/api/v1/me/password", staff, null,
                Map.of("currentPassword", "staff-password", "newPassword", "new-password-1")).status()).isEqualTo(204);
        assertThat(claims(signIn(email, "new-password-1").access()).get("email").asString()).isEqualTo(email);
    }

    // --- helpers ---

    private Response addStaff(String token, long venue, String email, String role) {
        return call("POST", "/api/v1/staff", token, venue,
                Map.of("email", email, "displayName", "Test " + role, "role", role, "password", "staff-password"));
    }

    private Tokens signIn(String email, String password) {
        Browser browser = new Browser();
        Pkce pkce = pkce();
        Response login = browser.startSignIn(pkce).submit(email, password);
        assertThat(login.location()).contains("/oauth2/authorize");

        Response authorized = browser.get(login.location());
        assertThat(authorized.location()).startsWith(REDIRECT_URI);
        String code = query(authorized.location()).get("code");

        Response token = tokenRequest(Map.of("grant_type", "authorization_code", "code", code,
                "redirect_uri", REDIRECT_URI, "code_verifier", pkce.verifier()));
        assertThat(token.status()).isEqualTo(200);
        JsonNode json = token.json();
        return new Tokens(json.get("access_token").asString(), json.get("refresh_token").asString(), json.get("id_token").asString());
    }

    private Response tokenRequest(Map<String, String> params) {
        return send(HttpRequest.newBuilder(URI.create(base + "/oauth2/token"))
                .header("Authorization", basic(GATEWAY, GATEWAY_SECRET))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(params))));
    }

    private Response call(String method, String path, String token, Long venue, Object body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (venue != null) {
            request.header("X-Tenant-Id", String.valueOf(venue));
        }
        return send(request);
    }

    private static Response send(HttpRequest.Builder request) {
        return send(HttpClient.newHttpClient(), request);
    }

    private static Response send(HttpClient client, HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.headers().firstValue("Location").orElse(null), response.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode claims(String jwt) {
        return JSON.readTree(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static Map<String, String> query(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        Arrays.stream(URI.create(url).getRawQuery().split("&")).map(p -> p.split("=", 2))
                .forEach(p -> params.put(p[0], URLDecoder.decode(p[1], StandardCharsets.UTF_8)));
        return params;
    }

    private static Pkce pkce() {
        try {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return new Pkce(verifier, Base64.getUrlEncoder().withoutPadding().encodeToString(hash));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // a browser: keeps the sign-in page's session cookie, does not follow redirects
    private class Browser {

        private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        private String csrf;

        Browser startSignIn(Pkce pkce) {
            String authorize = base + "/oauth2/authorize?" + form(Map.of(
                    "response_type", "code", "client_id", GATEWAY, "scope", "openid profile",
                    "redirect_uri", REDIRECT_URI, "state", "state-1", "nonce", "nonce-1",
                    "code_challenge", pkce.challenge(), "code_challenge_method", "S256"));
            Response toLogin = get(authorize);
            assertThat(toLogin.status()).isEqualTo(302);
            Response page = get(toLogin.location());
            Matcher matcher = CSRF.matcher(page.body());
            assertThat(matcher.find()).as("CSRF token on the sign-in page").isTrue();
            csrf = matcher.group(1);
            return this;
        }

        Response submit(String email, String password) {
            return send(client, HttpRequest.newBuilder(URI.create(base + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form(Map.of("username", email, "password", password, "_csrf", csrf)))));
        }

        Response get(String url) {
            return send(client, HttpRequest.newBuilder(URI.create(url)).header("Accept", "text/html").GET());
        }
    }

    private record Pkce(String verifier, String challenge) {
    }

    private record Tokens(String access, String refresh, String id) {
    }

    record Response(int status, String location, String body) {
        JsonNode json() {
            return JSON.readTree(body);
        }
    }
}
