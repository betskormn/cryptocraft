package com.cryptocraft.orderservice;

import com.cryptocraft.orderservice.clients.InventoryClient;
import com.cryptocraft.orderservice.clients.PricingClient;
import com.cryptocraft.orderservice.clients.dto.CurrentPriceDto;
import com.cryptocraft.orderservice.clients.dto.SellableItemDto;
import com.cryptocraft.orderservice.clients.dto.StockConsumptionRequestDto;
import com.cryptocraft.orderservice.clients.dto.TenantDto;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import com.cryptocraft.orderservice.scheduler.StockWriteOffRetrier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tables, visits, orders and bills against a real PostgreSQL with the production role split
 * (migrations as superuser, service as the RLS-bound role). Inventory and Pricing are mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// no application-test.yml: an explicit profile only keeps spring.profiles.default (local) from applying
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    private static final String ORDER_DB_PASSWORD = "test-order-password";
    private static final long VENUE_A = 1L;
    private static final long VENUE_B = 2L;
    private static final long UNKNOWN_VENUE = 999L;
    private static final long NEGRONI = 10L;
    private static final long FRIES = 20L;

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.parameters.order_db_password", () -> ORDER_DB_PASSWORD);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "cryptocraft_order");
        registry.add("spring.datasource.password", () -> ORDER_DB_PASSWORD);
        registry.add("eureka.client.enabled", () -> "false");
        // no Kafka in these tests; OrderPlaced is a best-effort notification
        registry.add("order.events.enabled", () -> "false");
        // the retry job is called directly
        registry.add("order.stock-retry-ms", () -> "3600000");
        registry.add("order.stock-retry-min-age-seconds", () -> "0");
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PricingClient pricingClient;

    @Autowired
    private StockWriteOffRetrier stockWriteOffRetrier;

    @LocalServerPort
    private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.create("http://localhost:" + port);
        reset(inventoryClient, pricingClient);
        when(inventoryClient.getTenant(anyLong())).thenReturn(Optional.empty());
        when(inventoryClient.getTenant(VENUE_A)).thenReturn(Optional.of(new TenantDto(VENUE_A, "ACTIVE")));
        when(inventoryClient.getTenant(VENUE_B)).thenReturn(Optional.of(new TenantDto(VENUE_B, "ACTIVE")));
        menu(true);
        prices("120.50", "90");
        doNothing().when(inventoryClient).writeOffStock(anyLong(), any());
    }

    // --- tables and visits ---

    @Test
    void guestsScanningTheSameTableJoinOneVisit() {
        String code = createTable(VENUE_A, "Table " + unique());

        JsonNode first = join(VENUE_A, code, "Anna").json();
        JsonNode second = join(VENUE_A, code, "Bohdan").json();

        assertThat(second.get("sessionId").asString()).isEqualTo(first.get("sessionId").asString());
        assertThat(second.get("participantId").asString()).isNotEqualTo(first.get("participantId").asString());
    }

    @Test
    void unknownOrInactiveTableCannotBeJoined() {
        assertThat(join(VENUE_A, "NOSUCHCODE12", null).status()).isEqualTo(404);

        Response table = call(HttpMethod.POST, "/api/v1/tables", VENUE_A, null,
                Map.of("label", "Broken " + unique(), "active", false));
        assertThat(join(VENUE_A, table.json().get("code").asString(), null).status()).isEqualTo(409);
    }

    @Test
    void unknownVenueIsRejected() {
        assertThat(call(HttpMethod.GET, "/api/v1/tables", UNKNOWN_VENUE, null, null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/tables", null, null, null).status()).isEqualTo(400);
    }

    // --- orders ---

    @Test
    void orderLocksTheCurrentPrices() {
        UUID guest = joinNewTable(VENUE_A);

        Response placed = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 2, FRIES, 1));
        assertThat(placed.status()).isEqualTo(201);
        assertThat(decimal(placed.json().get("total"))).isEqualByComparingTo("331.00");

        prices("200", "200");
        JsonNode reloaded = call(HttpMethod.GET, "/api/v1/orders/" + placed.json().get("orderId").asString(), VENUE_A, null, null).json();
        assertThat(decimal(reloaded.get("total"))).isEqualByComparingTo("331.00");
        JsonNode negroniLine = StreamSupport.stream(reloaded.get("lines").spliterator(), false)
                .filter(line -> line.get("itemId").asLong() == NEGRONI)
                .findFirst()
                .orElseThrow();
        assertThat(decimal(negroniLine.get("unitPrice"))).isEqualByComparingTo("120.50");
    }

    @Test
    void resendingTheSameCartDoesNotOrderTwice() {
        UUID guest = joinNewTable(VENUE_A);
        String clientOrderId = UUID.randomUUID().toString();

        Response first = placeOrder(VENUE_A, guest, clientOrderId, Map.of(NEGRONI, 1));
        Response second = placeOrder(VENUE_A, guest, clientOrderId, Map.of(NEGRONI, 1));

        assertThat(first.status()).isEqualTo(201);
        assertThat(second.status()).isEqualTo(200);
        assertThat(second.json().get("orderId").asString()).isEqualTo(first.json().get("orderId").asString());
        assertThat(myBill(VENUE_A, guest).get("orders").size()).isEqualTo(1);
    }

    @Test
    void stoppedItemOrMissingPriceIsRejected() {
        UUID guest = joinNewTable(VENUE_A);

        menu(false);
        Response stopped = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1));
        assertThat(stopped.status()).isEqualTo(409);
        assertThat(stopped.json().get("detail").asString()).contains("Negroni");

        menu(true);
        when(pricingClient.getCurrentPrices(any())).thenReturn(List.of());
        assertThat(placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).status()).isEqualTo(409);

        assertThat(placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(777L, 1)).status()).isEqualTo(404);
    }

    @Test
    void limitsKeepAbsurdOrdersOut() {
        UUID guest = joinNewTable(VENUE_A);

        assertThat(placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 11)).status()).isEqualTo(400);

        List<Map<String, Object>> sameItemTwice = List.of(
                Map.of("itemId", NEGRONI, "quantity", 6), Map.of("itemId", NEGRONI, "quantity", 5));
        assertThat(call(HttpMethod.POST, "/api/v1/orders", VENUE_A, guest,
                Map.of("clientOrderId", UUID.randomUUID().toString(), "lines", sameItemTwice)).status()).isEqualTo(400);

        List<Map<String, Object>> tooManyLines = new ArrayList<>();
        IntStream.range(0, 31).forEach(i -> tooManyLines.add(Map.of("itemId", 1000L + i, "quantity", 1)));
        assertThat(call(HttpMethod.POST, "/api/v1/orders", VENUE_A, guest,
                Map.of("clientOrderId", UUID.randomUUID().toString(), "lines", tooManyLines)).status()).isEqualTo(400);

        assertThat(call(HttpMethod.POST, "/api/v1/orders", VENUE_A, null,
                Map.of("clientOrderId", "x", "lines", List.of(Map.of("itemId", NEGRONI, "quantity", 1)))).status()).isEqualTo(400);
    }

    @Test
    void closedVisitTakesNoMoreOrders() {
        String code = createTable(VENUE_A, "Table " + unique());
        JsonNode joined = join(VENUE_A, code, null).json();
        UUID guest = UUID.fromString(joined.get("participantId").asString());

        Response closed = call(HttpMethod.POST, "/api/v1/table-sessions/" + joined.get("sessionId").asString() + "/close", VENUE_A, null, null);
        assertThat(closed.status()).isEqualTo(200);

        assertThat(placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).status()).isEqualTo(409);
        // the next guest at this table starts a new visit
        assertThat(join(VENUE_A, code, null).json().get("sessionId").asString()).isNotEqualTo(joined.get("sessionId").asString());
    }

    @Test
    void eachGuestHasTheirOwnBillWithTheirOwnPrices() {
        String code = createTable(VENUE_A, "Table " + unique());
        JsonNode anna = join(VENUE_A, code, "Anna").json();
        JsonNode bohdan = join(VENUE_A, code, "Bohdan").json();

        placeOrder(VENUE_A, UUID.fromString(anna.get("participantId").asString()), UUID.randomUUID().toString(), Map.of(NEGRONI, 1));
        prices("150", "90");
        placeOrder(VENUE_A, UUID.fromString(bohdan.get("participantId").asString()), UUID.randomUUID().toString(), Map.of(NEGRONI, 1));

        JsonNode bill = call(HttpMethod.GET, "/api/v1/table-sessions/" + anna.get("sessionId").asString() + "/bill", VENUE_A, null, null).json();
        assertThat(decimal(bill.get("total"))).isEqualByComparingTo("270.50");
        assertThat(decimal(bill.get("participants").get(0).get("total"))).isEqualByComparingTo("120.50");
        assertThat(decimal(bill.get("participants").get(1).get("total"))).isEqualByComparingTo("150");
    }

    // --- stock write-off ---

    @Test
    void stockIsWrittenOffAfterTheOrderIsPlaced() {
        UUID guest = joinNewTable(VENUE_A);

        JsonNode order = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 2)).json();

        ArgumentCaptor<StockConsumptionRequestDto> request = ArgumentCaptor.forClass(StockConsumptionRequestDto.class);
        verify(inventoryClient).writeOffStock(eq(VENUE_A), request.capture());
        assertThat(request.getValue().orderId()).isEqualTo(order.get("orderId").asString());
        assertThat(request.getValue().lines()).containsExactly(new StockConsumptionRequestDto.Line(NEGRONI, 2));
        assertThat(orderStockStatus(order)).isEqualTo("WRITTEN_OFF");
    }

    @Test
    void failedWriteOffIsRetriedLater() {
        UUID guest = joinNewTable(VENUE_A);
        doThrow(new DownstreamUnavailableException("Stock write-off failed", null))
                .when(inventoryClient).writeOffStock(anyLong(), any());

        JsonNode order = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();
        assertThat(orderStockStatus(order)).isEqualTo("PENDING");

        doNothing().when(inventoryClient).writeOffStock(anyLong(), any());
        stockWriteOffRetrier.retryPendingWriteOffs();

        assertThat(orderStockStatus(order)).isEqualTo("WRITTEN_OFF");
    }

    // --- venue isolation ---

    @Test
    void venuesDoNotSeeEachOthersTablesGuestsOrBills() {
        String code = createTable(VENUE_A, "Table " + unique());
        JsonNode joined = join(VENUE_A, code, null).json();
        UUID guest = UUID.fromString(joined.get("participantId").asString());
        placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1));

        assertThat(join(VENUE_B, code, null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/participants/me/bill", VENUE_B, guest, null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/table-sessions/" + joined.get("sessionId").asString() + "/bill", VENUE_B, null, null).status())
                .isEqualTo(404);
        assertThat(placeOrder(VENUE_B, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).status()).isEqualTo(404);
    }

    // --- helpers ---

    private void menu(boolean available) {
        when(inventoryClient.getSellableItems(anyLong())).thenReturn(List.of(
                new SellableItemDto(NEGRONI, "Negroni", "DRINK", available),
                new SellableItemDto(FRIES, "French Fries", "MEAL", true)));
    }

    private void prices(String negroni, String fries) {
        when(pricingClient.getCurrentPrices(any())).thenAnswer(invocation -> List.of(
                new CurrentPriceDto(NEGRONI, VENUE_A, new BigDecimal(negroni), false),
                new CurrentPriceDto(FRIES, VENUE_A, new BigDecimal(fries), false),
                new CurrentPriceDto(NEGRONI, VENUE_B, new BigDecimal(negroni), false)));
    }

    private String createTable(long venue, String label) {
        Response response = call(HttpMethod.POST, "/api/v1/tables", venue, null, Map.of("label", label));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("code").asString();
    }

    private Response join(long venue, String code, String name) {
        Map<String, Object> body = name == null ? Map.of("tableCode", code) : Map.of("tableCode", code, "displayName", name);
        return call(HttpMethod.POST, "/api/v1/table-sessions/join", venue, null, body);
    }

    private UUID joinNewTable(long venue) {
        return UUID.fromString(join(venue, createTable(venue, "Table " + unique()), null).json().get("participantId").asString());
    }

    private Response placeOrder(long venue, UUID participantId, String clientOrderId, Map<Long, Integer> quantities) {
        List<Map<String, Object>> lines = quantities.entrySet().stream()
                .map(e -> Map.<String, Object>of("itemId", e.getKey(), "quantity", e.getValue()))
                .toList();
        return call(HttpMethod.POST, "/api/v1/orders", venue, participantId, Map.of("clientOrderId", clientOrderId, "lines", lines));
    }

    private JsonNode myBill(long venue, UUID participantId) {
        return call(HttpMethod.GET, "/api/v1/participants/me/bill", venue, participantId, null).json();
    }

    private String orderStockStatus(JsonNode order) {
        return call(HttpMethod.GET, "/api/v1/orders/" + order.get("orderId").asString(), VENUE_A, null, null)
                .json().get("stockStatus").asString();
    }

    private static BigDecimal decimal(JsonNode node) {
        return new BigDecimal(node.asString());
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Response call(HttpMethod method, String path, Long venue, UUID participantId, Object body) {
        RestClient.RequestBodySpec request = http.method(method).uri(path);
        if (venue != null) {
            request.header("X-Tenant-Id", String.valueOf(venue));
        }
        if (participantId != null) {
            request.header("X-Participant-Id", participantId.toString());
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(JSON.writeValueAsString(body));
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
