package com.cryptocraft.inventoryservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fractional quantities, stop-list, menu order, sellable items and recipe write-offs.
 * Each test works in its own empty venue.
 */
class InventoryFoundationTest extends AbstractInventoryIntegrationTest {

    private long venue;
    private long brand;

    @BeforeEach
    void createEmptyVenue() {
        venue = createVenue(false);
        brand = createBrand("Brand");
    }

    // --- fractional quantities ---

    @Test
    void stockAndRecipesUseFractionalQuantities() {
        long gin = createProduct("Gin", "LITERS", "0.7");
        long item = createItem("Gin shot", List.of(line(gin, "0.05")), null);

        assertThat(sellable(item).get("portions").asInt()).isEqualTo(14);

        Response written = call(HttpMethod.PATCH, "/api/v1/products/" + gin + "/stock", venue, Map.of("delta", new BigDecimal("-0.25")));
        assertThat(written.status()).isEqualTo(200);
        assertThat(stockOf(gin)).isEqualByComparingTo("0.45");
    }

    @Test
    void moreThanThreeDecimalsAreRejected() {
        long gin = createProduct("Gin", "LITERS", "1");

        assertThat(call(HttpMethod.PATCH, "/api/v1/products/" + gin + "/stock", venue, Map.of("delta", new BigDecimal("0.0001"))).status())
                .isEqualTo(400);
        assertThat(call(HttpMethod.POST, "/api/v1/items", venue, itemBody("Bad", List.of(line(gin, "0.0005")), null)).status())
                .isEqualTo(400);
    }

    // --- stop-list and menu order ---

    @Test
    void stopListMarksItemUnavailableAndSurvivesAFullUpdate() {
        long lime = createProduct("Lime", "PIECE", "10");
        long item = createItem("Lemonade", List.of(line(lime, "1")), null);

        Response stopped = call(HttpMethod.PATCH, "/api/v1/items/" + item + "/stop-list", venue, Map.of("stopped", true));
        assertThat(stopped.status()).isEqualTo(200);
        assertThat(stopped.json().get("stopped").asBoolean()).isTrue();
        assertThat(sellable(item).get("available").asBoolean()).isFalse();

        Response updated = call(HttpMethod.PUT, "/api/v1/items/" + item, venue, itemBody("Lemonade", List.of(line(lime, "1")), 3));
        assertThat(updated.json().get("stopped").asBoolean()).isTrue();

        call(HttpMethod.PATCH, "/api/v1/items/" + item + "/stop-list", venue, Map.of("stopped", false));
        assertThat(sellable(item).get("available").asBoolean()).isTrue();
    }

    @Test
    void sellableItemsComeInMenuOrder() {
        long lime = createProduct("Lime", "PIECE", "10");
        createItem("Zero order", List.of(line(lime, "1")), null);
        createItem("Second", List.of(line(lime, "1")), 2);
        createItem("Alpha unordered", List.of(line(lime, "1")), null);
        createItem("First", List.of(line(lime, "1")), 1);

        List<String> names = StreamSupport.stream(call(HttpMethod.GET, "/api/v1/items/sellable", venue, null).json().spliterator(), false)
                .map(node -> node.get("name").asString())
                .toList();
        assertThat(names).containsExactly("First", "Second", "Alpha unordered", "Zero order");
    }

    @Test
    void sellableItemsCanBeLimitedToGivenIds() {
        long lime = createProduct("Lime", "PIECE", "10");
        long lemonade = createItem("Lemonade", List.of(line(lime, "1")), null);
        createItem("Other", List.of(line(lime, "1")), null);

        List<Long> ids = StreamSupport.stream(
                        call(HttpMethod.GET, "/api/v1/items/sellable?itemIds=" + lemonade + ",999999", venue, null).json().spliterator(), false)
                .map(node -> node.get("itemId").asLong())
                .toList();
        assertThat(ids).containsExactly(lemonade);
    }

    @Test
    void portionEstimateDoesNotOverflow() {
        long salt = createProduct("Salt", "GRAMS", "900000000");
        long item = createItem("Pinch", List.of(line(salt, "0.001")), null);

        assertThat(sellable(item).get("portions").asInt()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void internalPricesCarryMenuOrderAndStopList() {
        long lime = createProduct("Lime", "PIECE", "10");
        long item = createItem("Lemonade", List.of(line(lime, "1")), 7);
        call(HttpMethod.PATCH, "/api/v1/items/" + item + "/stop-list", venue, Map.of("stopped", true));

        JsonNode price = StreamSupport.stream(call(HttpMethod.GET, "/internal/prices", null, null).json().spliterator(), false)
                .filter(node -> node.get("itemId").asLong() == item)
                .findFirst()
                .orElseThrow();
        assertThat(price.get("displayOrder").asInt()).isEqualTo(7);
        assertThat(price.get("stopped").asBoolean()).isTrue();
    }

    // --- recipe write-offs ---

    @Test
    void consumptionWritesOffRecipesOfAllItems() {
        long gin = createProduct("Gin", "MILLILITERS", "1000");
        long tonic = createProduct("Tonic", "BOTTLES", "10");
        long ginTonic = createItem("Gin tonic", List.of(line(gin, "50"), line(tonic, "1")), null);
        long ginShot = createItem("Gin shot", List.of(line(gin, "40")), null);

        Response response = consume("order-1", Map.of(ginTonic, 3, ginShot, 1));

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.json().get("hasShortages").asBoolean()).isFalse();
        assertThat(stockOf(gin)).isEqualByComparingTo("810");
        assertThat(stockOf(tonic)).isEqualByComparingTo("7");
    }

    @Test
    void repeatedConsumptionForTheSameOrderChangesNothing() {
        long gin = createProduct("Gin", "MILLILITERS", "1000");
        long ginShot = createItem("Gin shot", List.of(line(gin, "40")), null);

        assertThat(consume("order-1", Map.of(ginShot, 2)).status()).isEqualTo(201);
        Response repeated = consume("order-1", Map.of(ginShot, 2));

        assertThat(repeated.status()).isEqualTo(200);
        assertThat(repeated.json().get("orderId").asString()).isEqualTo("order-1");
        assertThat(stockOf(gin)).isEqualByComparingTo("920");
    }

    @Test
    void shortageIsReportedButDoesNotBlockTheOrder() {
        long gin = createProduct("Gin", "LITERS", "0.1");
        long ginShot = createItem("Gin shot", List.of(line(gin, "0.05")), null);

        Response response = consume("order-1", Map.of(ginShot, 3));

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.json().get("hasShortages").asBoolean()).isTrue();
        JsonNode line = response.json().get("lines").get(0);
        assertThat(new BigDecimal(line.get("consumed").asString())).isEqualByComparingTo("0.1");
        assertThat(new BigDecimal(line.get("shortage").asString())).isEqualByComparingTo("0.05");
        assertThat(stockOf(gin)).isEqualByComparingTo("0");
    }

    @Test
    void consumptionOfUnknownItemIsNotFound() {
        Response response = consume("order-1", Map.of(999999L, 1));

        assertThat(response.status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/stock-consumptions/order-1", venue, null).status()).isEqualTo(404);
    }

    @Test
    void orderIdsAreScopedToTheVenue() {
        long gin = createProduct("Gin", "MILLILITERS", "1000");
        long ginShot = createItem("Gin shot", List.of(line(gin, "40")), null);
        assertThat(consume("order-1", Map.of(ginShot, 1)).status()).isEqualTo(201);

        long otherVenue = createVenue(false);
        assertThat(call(HttpMethod.GET, "/api/v1/stock-consumptions/order-1", otherVenue, null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/stock-consumptions/order-1", venue, null).status()).isEqualTo(200);
    }

    @Test
    void demoMenuKeepsTheTemplateMenuOrder() {
        long templateId = call(HttpMethod.GET, "/api/v1/platform/tenants/by-slug/demo-template", null, null)
                .json().get("tenantId").asLong();
        long demoVenue = createVenue(true);

        assertThat(menuOrder(demoVenue)).isNotEmpty().isEqualTo(menuOrder(templateId));
    }

    // --- helpers ---

    private long createBrand(String name) {
        Response response = call(HttpMethod.POST, "/api/v1/brands", venue, Map.of("name", name));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("brandId").asLong();
    }

    private long createProduct(String name, String unit, String quantity) {
        Response response = call(HttpMethod.POST, "/api/v1/products", venue, Map.of(
                "name", name, "description", "test", "quantity", new BigDecimal(quantity), "unit", unit, "brandId", brand));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("productId").asLong();
    }

    private long createItem(String name, List<Map<String, Object>> products, Integer displayOrder) {
        Response response = call(HttpMethod.POST, "/api/v1/items", venue, itemBody(name, products, displayOrder));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("itemId").asLong();
    }

    private Response consume(String orderId, Map<Long, Integer> portionsByItem) {
        List<Map<String, Object>> lines = portionsByItem.entrySet().stream()
                .map(e -> Map.<String, Object>of("itemId", e.getKey(), "quantity", e.getValue()))
                .toList();
        return call(HttpMethod.POST, "/api/v1/stock-consumptions", venue, Map.of("orderId", orderId, "lines", lines));
    }

    private JsonNode sellable(long itemId) {
        return StreamSupport.stream(call(HttpMethod.GET, "/api/v1/items/sellable", venue, null).json().spliterator(), false)
                .filter(node -> node.get("itemId").asLong() == itemId)
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal stockOf(long productId) {
        return new BigDecimal(call(HttpMethod.GET, "/api/v1/products/" + productId, venue, null)
                .json().get("quantityInStock").asString());
    }

    // item names in sellable order, for venues whose ids are not known up front
    private List<String> menuOrder(long tenantId) {
        return StreamSupport.stream(call(HttpMethod.GET, "/api/v1/items/sellable", tenantId, null).json().spliterator(), false)
                .map(node -> node.get("name").asString())
                .toList();
    }

    private static Map<String, Object> line(long productId, String quantity) {
        return Map.of("productId", productId, "quantity", new BigDecimal(quantity));
    }

    private static Map<String, Object> itemBody(String name, List<?> products, Integer displayOrder) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "name", name, "type", "DRINK", "basePrice", 100, "minPrice", 80, "maxPrice", 150, "products", products));
        if (displayOrder != null) {
            body.put("displayOrder", displayOrder);
        }
        return body;
    }
}
