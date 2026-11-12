package com.cryptocraft.orderservice;

import com.cryptocraft.orderservice.clients.IdentityClient;
import com.cryptocraft.orderservice.clients.InventoryClient;
import com.cryptocraft.orderservice.clients.PricingClient;
import com.cryptocraft.orderservice.clients.dto.CurrentPriceDto;
import com.cryptocraft.orderservice.clients.dto.GuestTokenDto;
import com.cryptocraft.orderservice.clients.dto.SellableItemDto;
import com.cryptocraft.orderservice.clients.dto.StockConsumptionRequestDto;
import com.cryptocraft.orderservice.clients.dto.TenantDto;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import com.cryptocraft.orderservice.exceptions.StockWriteOffRejectedException;
import com.cryptocraft.orderservice.scheduler.StockWriteOffRetrier;
import com.cryptocraft.security.test.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
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
        // tokens are signed by TestTokens instead of identity-service
        registry.add("cryptocraft.security.public-key", TestTokens::publicKey);
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PricingClient pricingClient;

    // signs guest tokens like identity-service would
    @MockitoBean
    private IdentityClient identityClient;

    @Autowired
    private StockWriteOffRetrier stockWriteOffRetrier;

    @LocalServerPort
    private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.create("http://localhost:" + port);
        reset(inventoryClient, pricingClient, identityClient);
        when(identityClient.issueGuestToken(anyLong(), any(), any())).thenAnswer(call -> new GuestTokenDto(
                TestTokens.guest(call.getArgument(0, Long.class), call.getArgument(1, UUID.class)), Instant.now().plusSeconds(3600)));
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
        // a guest who does not say which venue
        assertThat(callAs(null, HttpMethod.POST, "/api/v1/table-sessions/join", null, Map.of("tableCode", "ANYCODE12345")).status())
                .isEqualTo(400);
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

        // not a guest: only guests order with their own token
        assertThat(call(HttpMethod.POST, "/api/v1/orders", VENUE_A, null,
                Map.of("clientOrderId", "x", "lines", List.of(Map.of("itemId", NEGRONI, "quantity", 1)))).status()).isEqualTo(403);
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

        // the guest does not wait for the write-off: it happens right after, on another thread
        assertThat(order.get("stockStatus").asString()).isEqualTo("PENDING");
        ArgumentCaptor<StockConsumptionRequestDto> request = ArgumentCaptor.forClass(StockConsumptionRequestDto.class);
        verify(inventoryClient, timeout(5000)).writeOffStock(eq(VENUE_A), request.capture());
        assertThat(request.getValue().orderId()).isEqualTo(order.get("orderId").asString());
        assertThat(request.getValue().lines()).containsExactly(new StockConsumptionRequestDto.Line(NEGRONI, 2));
        awaitStockStatus(order, "WRITTEN_OFF");
    }

    @Test
    void failedWriteOffIsRetriedLater() {
        UUID guest = joinNewTable(VENUE_A);
        doThrow(new DownstreamUnavailableException("Stock write-off failed", null))
                .when(inventoryClient).writeOffStock(anyLong(), any());

        JsonNode order = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();
        verify(inventoryClient, timeout(5000)).writeOffStock(anyLong(), any());
        assertThat(orderStockStatus(order)).isEqualTo("PENDING");

        doNothing().when(inventoryClient).writeOffStock(anyLong(), any());
        stockWriteOffRetrier.retryPendingWriteOffs();

        assertThat(orderStockStatus(order)).isEqualTo("WRITTEN_OFF");
    }

    @Test
    void writeOffRefusedByInventoryIsNotRetried() {
        UUID guest = joinNewTable(VENUE_A);
        doThrow(new StockWriteOffRejectedException("Stock write-off rejected: item deleted", null))
                .when(inventoryClient).writeOffStock(anyLong(), any());

        JsonNode order = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();
        awaitStockStatus(order, "FAILED");

        stockWriteOffRetrier.retryPendingWriteOffs();
        verify(inventoryClient, times(1)).writeOffStock(anyLong(), any());
        assertThat(orderStockStatus(order)).isEqualTo("FAILED");
    }

    @Test
    void menuIsAskedOnlyForTheOrderedItems() {
        UUID guest = joinNewTable(VENUE_A);

        placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1));

        verify(inventoryClient).getSellableItems(eq(VENUE_A), eq(Set.of(NEGRONI)));
    }

    // --- kitchen / bar screens ---

    @Test
    void eachStationSeesItsOwnLinesInTheOrderTheyWereSent() {
        String code = createTable(VENUE_A, "Table " + unique());
        JsonNode guest = join(VENUE_A, code, "Anna").json();
        UUID anna = UUID.fromString(guest.get("participantId").asString());
        JsonNode first = placeOrder(VENUE_A, anna, UUID.randomUUID().toString(), Map.of(NEGRONI, 1, FRIES, 2)).json();
        JsonNode second = placeOrder(VENUE_A, anna, UUID.randomUUID().toString(), Map.of(FRIES, 1)).json();

        List<JsonNode> kitchen = queueOf(VENUE_A, "MEAL", first, second);
        assertThat(kitchen).extracting(line -> line.get("orderId").asString())
                .containsExactly(first.get("orderId").asString(), second.get("orderId").asString());
        JsonNode fries = kitchen.getFirst();
        assertThat(fries.get("itemName").asString()).isEqualTo("French Fries");
        assertThat(fries.get("quantity").asInt()).isEqualTo(2);
        assertThat(fries.get("status").asString()).isEqualTo("NEW");
        assertThat(fries.get("tableLabel").asString()).isEqualTo(guest.get("tableLabel").asString());
        assertThat(fries.get("guestName").asString()).isEqualTo("Anna");

        assertThat(queueOf(VENUE_A, "DRINK", first, second)).extracting(line -> line.get("itemName").asString())
                .containsExactly("Negroni");
    }

    @Test
    void lineMovesAcrossTheScreenAndLeavesItWhenServed() {
        JsonNode order = placeOrder(VENUE_A, joinNewTable(VENUE_A), UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();
        long line = lineId(order, "Negroni");

        assertThat(changeStatus(VENUE_A, line, "PREPARING", null).json().get("status").asString()).isEqualTo("PREPARING");
        assertThat(changeStatus(VENUE_A, line, "READY", null).status()).isEqualTo(200);
        // a mis-tap is undone by moving the line back
        assertThat(changeStatus(VENUE_A, line, "PREPARING", null).status()).isEqualTo(200);
        assertThat(changeStatus(VENUE_A, line, "SERVED", null).json().get("statusChangedAt").isNull()).isFalse();

        assertThat(queueOf(VENUE_A, "DRINK", order)).isEmpty();
        // the same tap from a second screen changes nothing
        assertThat(changeStatus(VENUE_A, line, "SERVED", null).status()).isEqualTo(200);
        assertThat(changeStatus(VENUE_A, line, "READY", null).status()).isEqualTo(409);
        assertThat(changeStatus(VENUE_A, line, "CANCELLED", null).status()).isEqualTo(409);
    }

    @Test
    void cancelledLineIsTakenOffTheGuestsBill() {
        UUID guest = joinNewTable(VENUE_A);
        JsonNode order = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 2, FRIES, 1)).json();
        assertThat(decimal(myBill(VENUE_A, guest).get("total"))).isEqualByComparingTo("331");

        Response cancelled = changeStatus(VENUE_A, lineId(order, "French Fries"), "CANCELLED", " Ran out ");
        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(queueOf(VENUE_A, "MEAL", order)).isEmpty();

        JsonNode bill = myBill(VENUE_A, guest);
        assertThat(decimal(bill.get("total"))).isEqualByComparingTo("241");
        JsonNode billOrder = bill.get("orders").get(0);
        assertThat(decimal(billOrder.get("total"))).isEqualByComparingTo("241");
        JsonNode fries = StreamSupport.stream(billOrder.get("lines").spliterator(), false)
                .filter(l -> l.get("itemName").asString().equals("French Fries"))
                .findFirst()
                .orElseThrow();
        assertThat(fries.get("status").asString()).isEqualTo("CANCELLED");
        assertThat(fries.get("cancelReason").asString()).isEqualTo("Ran out");

        // nothing left to charge: the whole order is cancelled
        changeStatus(VENUE_A, lineId(order, "Negroni"), "CANCELLED", null);
        bill = myBill(VENUE_A, guest);
        assertThat(decimal(bill.get("total"))).isEqualByComparingTo("0");
        assertThat(bill.get("orders").get(0).get("status").asString()).isEqualTo("CANCELLED");
    }

    @Test
    void invalidStatusOrStationIsRejected() {
        JsonNode order = placeOrder(VENUE_A, joinNewTable(VENUE_A), UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();

        assertThat(changeStatus(VENUE_A, lineId(order, "Negroni"), "BURNT", null).status()).isEqualTo(400);
        assertThat(changeStatus(VENUE_A, 99_999_999L, "READY", null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/kitchen/lines?itemType=COFFEE", VENUE_A, null, null).status()).isEqualTo(400);
    }

    @Test
    void venuesDoNotSeeEachOthersKitchen() {
        JsonNode order = placeOrder(VENUE_A, joinNewTable(VENUE_A), UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();

        assertThat(queueOf(VENUE_B, "DRINK", order)).isEmpty();
        assertThat(changeStatus(VENUE_B, lineId(order, "Negroni"), "READY", null).status()).isEqualTo(404);
        assertThat(queueOf(VENUE_A, "DRINK", order)).hasSize(1);
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
        when(inventoryClient.getSellableItems(anyLong(), any())).thenReturn(List.of(
                new SellableItemDto(NEGRONI, "Negroni", "DRINK", available),
                new SellableItemDto(FRIES, "French Fries", "MEAL", true)));
    }

    private void prices(String negroni, String fries) {
        when(pricingClient.getCurrentPrices(any())).thenAnswer(invocation -> List.of(
                new CurrentPriceDto(NEGRONI, VENUE_A, new BigDecimal(negroni), false),
                new CurrentPriceDto(FRIES, VENUE_A, new BigDecimal(fries), false),
                new CurrentPriceDto(NEGRONI, VENUE_B, new BigDecimal(negroni), false)));
    }

    // --- access ---

    @Test
    void kitchenScreensNeedASignedInMemberOfTheVenue() {
        String queue = "/api/v1/kitchen/lines?itemType=DRINK";

        assertThat(callAs(null, HttpMethod.GET, queue, VENUE_A, null).status()).isEqualTo(401);
        assertThat(callAs("not-a-token", HttpMethod.GET, queue, VENUE_A, null).status()).isEqualTo(401);
        assertThat(callAs(TestTokens.venue(VENUE_B, "STAFF"), HttpMethod.GET, queue, VENUE_A, null).status()).isEqualTo(403);
        assertThat(callAs(TestTokens.venue(VENUE_A, "INVENTORY_MANAGER"), HttpMethod.GET, queue, VENUE_A, null).status()).isEqualTo(403);
        assertThat(callAs(TestTokens.venue(VENUE_A, "STAFF"), HttpMethod.GET, queue, VENUE_A, null).status()).isEqualTo(200);
        assertThat(callAs(TestTokens.platformAdmin(), HttpMethod.GET, queue, VENUE_A, null).status()).isEqualTo(200);
    }

    @Test
    void onlyTheVenueAdminManagesTables() {
        String staff = TestTokens.venue(VENUE_A, "STAFF");

        assertThat(callAs(staff, HttpMethod.POST, "/api/v1/tables", VENUE_A, Map.of("label", "Staff " + unique())).status())
                .isEqualTo(403);
        assertThat(callAs(staff, HttpMethod.GET, "/api/v1/table-sessions", VENUE_A, null).status()).isEqualTo(200);
    }

    @Test
    void guestsJoinWithoutAnAccountAndGetTheirToken() {
        String code = createTable(VENUE_A, "Table " + unique());

        Response joined = callAs(null, HttpMethod.POST, "/api/v1/table-sessions/join", VENUE_A, Map.of("tableCode", code));
        assertThat(joined.status()).isEqualTo(201);
        String token = joined.json().get("guestToken").asString();
        assertThat(joined.json().get("status").asString()).isEqualTo("PENDING");

        assertThat(callAs(token, HttpMethod.GET, "/api/v1/participants/me/bill", VENUE_A, null).status()).isEqualTo(200);
        assertThat(callAs(null, HttpMethod.GET, "/api/v1/participants/me/bill", VENUE_A, null).status()).isEqualTo(401);
    }

    @Test
    void aGuestTokenOpensOnlyThatGuestsThingsInThatVenue() {
        UUID guest = joinNewTable(VENUE_A);
        String token = TestTokens.guest(VENUE_A, guest);
        Map<String, Object> order = Map.of("clientOrderId", UUID.randomUUID().toString(), "lines", List.of(Map.of("itemId", NEGRONI, "quantity", 1)));

        assertThat(callAs(token, HttpMethod.GET, "/api/v1/participants/me/bill", VENUE_B, null).status()).isEqualTo(403);
        assertThat(callAs(token, HttpMethod.GET, "/api/v1/kitchen/lines?itemType=DRINK", VENUE_A, null).status()).isEqualTo(403);
        assertThat(callAs(token, HttpMethod.GET, "/api/v1/table-sessions", VENUE_A, null).status()).isEqualTo(403);
        // staff order for a guest through the waiter's screen later (stage 6), not with their own token
        assertThat(callAs(TestTokens.venue(VENUE_A, "STAFF"), HttpMethod.POST, "/api/v1/orders", VENUE_A, order).status()).isEqualTo(403);
    }

    @Test
    void scanningAgainWithTheTokenKeepsTheSameGuest() {
        String code = createTable(VENUE_A, "Table " + unique());
        JsonNode first = callAs(null, HttpMethod.POST, "/api/v1/table-sessions/join", VENUE_A, Map.of("tableCode", code)).json();

        JsonNode again = callAs(first.get("guestToken").asString(), HttpMethod.POST, "/api/v1/table-sessions/join", VENUE_A,
                Map.of("tableCode", code)).json();

        assertThat(again.get("participantId").asString()).isEqualTo(first.get("participantId").asString());
        assertThat(again.get("guestToken").asString()).isNotBlank();
    }

    // --- a waiter confirms new guests ---

    @Test
    void aNewGuestsOrderWaitsForTheWaiter() {
        String code = createTable(VENUE_A, "Table " + unique());
        JsonNode joined = joinUnconfirmed(VENUE_A, code);
        UUID guest = UUID.fromString(joined.get("participantId").asString());
        String sessionId = joined.get("sessionId").asString();

        JsonNode order = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();

        assertThat(order.get("status").asString()).isEqualTo("AWAITING_APPROVAL");
        assertThat(queueOf(VENUE_A, "DRINK", order)).isEmpty();
        assertThat(decimal(myBill(VENUE_A, guest).get("total"))).isEqualByComparingTo("120.50");
        assertThat(pendingGuests(VENUE_A, sessionId)).isEqualTo(1);
        stockWriteOffRetrier.retryPendingWriteOffs();
        verify(inventoryClient, times(0)).writeOffStock(eq(VENUE_A), argThat(writeOffOf(order)));

        Response approved = call(HttpMethod.POST, "/api/v1/table-sessions/" + sessionId + "/participants/" + guest + "/approve", VENUE_A, null, null);

        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.json().get("status").asString()).isEqualTo("APPROVED");
        assertThat(queueOf(VENUE_A, "DRINK", order)).hasSize(1);
        verify(inventoryClient, timeout(5000)).writeOffStock(eq(VENUE_A), argThat(writeOffOf(order)));
        assertThat(pendingGuests(VENUE_A, sessionId)).isZero();
        // the next order goes straight to the bar
        JsonNode next = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).json();
        assertThat(next.get("status").asString()).isEqualTo("PLACED");
    }

    @Test
    void aRejectedGuestsOrdersAreCancelledAndTheyCannotOrder() {
        JsonNode joined = joinUnconfirmed(VENUE_A, createTable(VENUE_A, "Table " + unique()));
        UUID guest = UUID.fromString(joined.get("participantId").asString());
        String guestPath = "/api/v1/table-sessions/" + joined.get("sessionId").asString() + "/participants/" + guest;
        JsonNode held = placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 2)).json();

        Response rejected = callAs(TestTokens.venue(VENUE_A, "STAFF"), HttpMethod.POST, guestPath + "/reject", VENUE_A, null);

        assertThat(rejected.status()).isEqualTo(200);
        JsonNode bill = rejected.json();
        assertThat(bill.get("status").asString()).isEqualTo("REJECTED");
        assertThat(decimal(bill.get("total"))).isEqualByComparingTo("0");
        JsonNode line = bill.get("orders").get(0).get("lines").get(0);
        assertThat(line.get("status").asString()).isEqualTo("CANCELLED");
        assertThat(line.get("cancelReason").asString()).isEqualTo("Guest not confirmed by the staff");
        assertThat(placeOrder(VENUE_A, guest, UUID.randomUUID().toString(), Map.of(NEGRONI, 1)).status()).isEqualTo(409);
        assertThat(call(HttpMethod.POST, guestPath + "/approve", VENUE_A, null, null).status()).isEqualTo(409);
        verify(inventoryClient, times(0)).writeOffStock(eq(VENUE_A), argThat(writeOffOf(held)));
    }

    @Test
    void guestsAreConfirmedOnlyAtTheirOwnVisitByTheVenuesStaff() {
        JsonNode joined = joinUnconfirmed(VENUE_A, createTable(VENUE_A, "Table " + unique()));
        String guest = joined.get("participantId").asString();
        String otherSession = joinUnconfirmed(VENUE_A, createTable(VENUE_A, "Table " + unique())).get("sessionId").asString();

        assertThat(call(HttpMethod.POST, "/api/v1/table-sessions/" + otherSession + "/participants/" + guest + "/approve", VENUE_A, null, null)
                .status()).isEqualTo(404);
        assertThat(callAs(TestTokens.venue(VENUE_A, "INVENTORY_MANAGER"), HttpMethod.POST,
                "/api/v1/table-sessions/" + joined.get("sessionId").asString() + "/participants/" + guest + "/approve", VENUE_A, null)
                .status()).isEqualTo(403);
    }

    private String createTable(long venue, String label) {
        Response response = call(HttpMethod.POST, "/api/v1/tables", venue, null, Map.of("label", label));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("code").asString();
    }

    // a guest joins and a waiter confirms them, as most tests need
    private Response join(long venue, String code, String name) {
        Map<String, Object> body = name == null ? Map.of("tableCode", code) : Map.of("tableCode", code, "displayName", name);
        Response joined = callAs(null, HttpMethod.POST, "/api/v1/table-sessions/join", venue, body);
        if (joined.status() == 201) {
            JsonNode json = joined.json();
            Response approved = call(HttpMethod.POST, "/api/v1/table-sessions/" + json.get("sessionId").asString()
                    + "/participants/" + json.get("participantId").asString() + "/approve", venue, null, null);
            assertThat(approved.status()).isEqualTo(200);
        }
        return joined;
    }

    private JsonNode joinUnconfirmed(long venue, String code) {
        Response joined = callAs(null, HttpMethod.POST, "/api/v1/table-sessions/join", venue, Map.of("tableCode", code));
        assertThat(joined.status()).isEqualTo(201);
        return joined.json();
    }

    private static ArgumentMatcher<StockConsumptionRequestDto> writeOffOf(JsonNode order) {
        return request -> request != null && request.orderId().equals(order.get("orderId").asString());
    }

    private long pendingGuests(long venue, String sessionId) {
        return StreamSupport.stream(call(HttpMethod.GET, "/api/v1/table-sessions", venue, null, null).json().spliterator(), false)
                .filter(s -> s.get("sessionId").asString().equals(sessionId))
                .findFirst()
                .orElseThrow()
                .get("pendingGuests").asLong();
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

    // the station's queue limited to the given orders: other tests share the database
    private List<JsonNode> queueOf(long venue, String itemType, JsonNode... orders) {
        Set<String> orderIds = Arrays.stream(orders).map(o -> o.get("orderId").asString()).collect(Collectors.toSet());
        Response response = call(HttpMethod.GET, "/api/v1/kitchen/lines?itemType=" + itemType, venue, null, null);
        assertThat(response.status()).isEqualTo(200);
        return StreamSupport.stream(response.json().spliterator(), false)
                .filter(line -> orderIds.contains(line.get("orderId").asString()))
                .toList();
    }

    private Response changeStatus(long venue, long lineId, String status, String reason) {
        Map<String, Object> body = reason == null ? Map.of("status", status) : Map.of("status", status, "reason", reason);
        return call(HttpMethod.PATCH, "/api/v1/kitchen/lines/" + lineId + "/status", venue, null, body);
    }

    private static long lineId(JsonNode order, String itemName) {
        return StreamSupport.stream(order.get("lines").spliterator(), false)
                .filter(line -> line.get("itemName").asString().equals(itemName))
                .findFirst()
                .orElseThrow()
                .get("lineId").asLong();
    }

    private void awaitStockStatus(JsonNode order, String expected) {
        long deadline = System.currentTimeMillis() + 5000;
        while (!expected.equals(orderStockStatus(order)) && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(orderStockStatus(order)).isEqualTo(expected);
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

    // with a venue: as its admin; without: anonymous, like a guest
    // with a guest: as that guest (their token); with only a venue: as its admin; with neither: anonymous
    private Response call(HttpMethod method, String path, Long venue, UUID participantId, Object body) {
        String token = participantId != null ? TestTokens.guest(venue, participantId)
                : venue != null ? TestTokens.venue(venue, "ADMIN")
                : null;
        return callAs(token, method, path, venue, body);
    }

    private Response callAs(String token, HttpMethod method, String path, Long venue, Object body) {
        RestClient.RequestBodySpec request = http.method(method).uri(path);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (venue != null) {
            request.header("X-Tenant-Id", String.valueOf(venue));
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
