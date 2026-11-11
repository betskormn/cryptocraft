package com.cryptocraft.inventoryservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Admin API behaviour: error responses, guarded deletes, stock adjustments and updates.
 * Each test works in its own empty venue.
 */
class InventoryApiTest extends AbstractInventoryIntegrationTest {

    private long venue;

    @BeforeEach
    void createEmptyVenue() {
        venue = createVenue(false);
    }

    // --- error responses ---

    @Test
    void duplicatesAreConflicts() {
        long brand = createBrand("Bacardi");
        createProduct(brand, "White rum", 10);

        Response brandDuplicate = call(HttpMethod.POST, "/api/v1/brands", venue, Map.of("name", "bacardi"));
        assertProblem(brandDuplicate, 409, "Brand already exists: bacardi");

        Response productDuplicate = call(HttpMethod.POST, "/api/v1/products", venue, productBody(brand, "white RUM", 1));
        assertProblem(productDuplicate, 409, "Product already exists");
    }

    @Test
    void missingReferencesAreNotFound() {
        assertProblem(call(HttpMethod.POST, "/api/v1/items", venue, itemBody("Ghost", List.of(Map.of("productId", 999999, "quantity", 1)))),
                404, "Products not found: [999999]");
        assertThat(call(HttpMethod.GET, "/api/v1/items/999999", venue, null).status()).isEqualTo(404);
        assertThat(call(HttpMethod.GET, "/api/v1/no-such-endpoint", venue, null).status()).isEqualTo(404);
    }

    @Test
    void malformedRequestsAreBadRequests() {
        long brand = createBrand("Brand");

        String unknownUnit = """
                {"name":"X","description":"d","quantity":1,"unit":"BUCKETS","brandId":%d}""".formatted(brand);
        assertThat(call(HttpMethod.POST, "/api/v1/products", venue, unknownUnit).status()).isEqualTo(400);
        assertThat(call(HttpMethod.GET, "/api/v1/items/0", venue, null).status()).isEqualTo(400);

        Map<String, Object> badPrices = new HashMap<>(itemBody("Bad", List.of()));
        badPrices.put("minPrice", 500);
        badPrices.put("products", List.of(Map.of("productId", createProduct(brand, "P", 1), "quantity", 1)));
        Response response = call(HttpMethod.POST, "/api/v1/items", venue, badPrices);
        assertProblem(response, 400, "min <= base <= max");
        assertThat(response.json().get("errors").toString()).contains("min <= base <= max");
    }

    @Test
    void errorResponsesDoNotLeakInternals() {
        long brand = createBrand("Brand");
        createProduct(brand, "P", 1);
        Response response = call(HttpMethod.POST, "/api/v1/products", venue, productBody(brand, "P", 1));

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.body()).doesNotContainIgnoringCase("stackTrace").doesNotContainIgnoringCase("insert into");
    }

    // --- guarded deletes ---

    @Test
    void productUsedInRecipeCannotBeDeleted() {
        long brand = createBrand("Gordon's");
        long gin = createProduct(brand, "Gin", 1000);
        createItem("Gin tonic", List.of(Map.of("productId", gin, "quantity", 50)));

        assertProblem(call(HttpMethod.DELETE, "/api/v1/products/" + gin, venue, null), 409, "Gin tonic");
        assertThat(call(HttpMethod.GET, "/api/v1/products/" + gin, venue, null).status()).isEqualTo(200);
    }

    @Test
    void brandWithProductsCannotBeDeleted() {
        long brand = createBrand("Schweppes");
        long tonic = createProduct(brand, "Tonic", 10);

        assertProblem(call(HttpMethod.DELETE, "/api/v1/brands/" + brand, venue, null), 409, "has 1 product(s)");
        assertThat(call(HttpMethod.GET, "/api/v1/products/" + tonic, venue, null).status()).isEqualTo(200);

        long emptyBrand = createBrand("Unused");
        assertThat(call(HttpMethod.DELETE, "/api/v1/brands/" + emptyBrand, venue, null).status()).isEqualTo(204);
    }

    // --- stock ---

    @Test
    void stockCanBeWrittenOffButNotBelowZero() {
        long product = createProduct(createBrand("Brand"), "Lime", 0);

        assertThat(adjustStock(product, 10).json().get("quantityInStock").asInt()).isEqualTo(10);
        assertThat(adjustStock(product, -4).json().get("quantityInStock").asInt()).isEqualTo(6);

        assertProblem(adjustStock(product, -7), 409, "cannot go below zero");
        assertProblem(adjustStock(product, 0), 400, "delta must not be 0");
        assertThat(stockOf(product)).isEqualTo(6);
    }

    // --- updates ---

    @Test
    void productUpdateMovesBrandButNeverOverwritesStock() {
        long oldBrand = createBrand("Old");
        long newBrand = createBrand("New");
        long product = createProduct(oldBrand, "Cola", 5);

        Response response = call(HttpMethod.PUT, "/api/v1/products/" + product, venue, productBody(newBrand, " Cola Zero ", 999));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().get("brandId").asLong()).isEqualTo(newBrand);
        assertThat(response.json().get("name").asString()).isEqualTo("Cola Zero");
        assertThat(stockOf(product)).isEqualTo(5);
    }

    @Test
    void productCannotBeRenamedToAnExistingName() {
        long brand = createBrand("Brand");
        createProduct(brand, "Cola", 1);
        long other = createProduct(brand, "Fanta", 1);

        assertProblem(call(HttpMethod.PUT, "/api/v1/products/" + other, venue, productBody(brand, "cola", 1)),
                409, "Product already exists");
    }

    @Test
    void itemUpdateMergesTheSameProductListedTwice() {
        long brand = createBrand("Brand");
        long rum = createProduct(brand, "Rum", 100);
        long item = createItem("Cuba libre", List.of(Map.of("productId", rum, "quantity", 1)));

        Response response = call(HttpMethod.PUT, "/api/v1/items/" + item, venue, itemBody("Cuba libre", List.of(
                Map.of("productId", rum, "quantity", 1),
                Map.of("productId", rum, "quantity", 2))));

        assertThat(response.status()).isEqualTo(200);
        JsonNode products = response.json().get("products");
        assertThat(products.size()).isEqualTo(1);
        assertThat(products.get(0).get("quantity").asInt()).isEqualTo(3);
    }

    @Test
    void longRecipeIsAccepted() {
        long product = createProduct(createBrand("Brand"), "P", 1);
        Map<String, Object> body = new HashMap<>(itemBody("Long recipe", List.of(Map.of("productId", product, "quantity", 1))));
        body.put("recipe", "r".repeat(1500));
        body.put("image", "https://example.com/" + "i".repeat(400));

        assertThat(call(HttpMethod.POST, "/api/v1/items", venue, body).status()).isEqualTo(201);
    }

    @Test
    void brandCanBeRenamedButNotToAnExistingName() {
        long brand = createBrand("Jamesson");
        createBrand("Jameson");

        assertProblem(call(HttpMethod.PUT, "/api/v1/brands/" + brand, venue, Map.of("name", "JAMESON")), 409, "Brand already exists");

        Response renamed = call(HttpMethod.PUT, "/api/v1/brands/" + brand, venue, Map.of("name", " Jameson Black "));
        assertThat(renamed.status()).isEqualTo(200);
        assertThat(renamed.json().get("name").asString()).isEqualTo("Jameson Black");
    }

    // --- helpers ---

    private long createBrand(String name) {
        Response response = call(HttpMethod.POST, "/api/v1/brands", venue, Map.of("name", name));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("brandId").asLong();
    }

    private long createProduct(long brandId, String name, int quantity) {
        Response response = call(HttpMethod.POST, "/api/v1/products", venue, productBody(brandId, name, quantity));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("productId").asLong();
    }

    private long createItem(String name, List<Map<String, Object>> products) {
        Response response = call(HttpMethod.POST, "/api/v1/items", venue, itemBody(name, products));
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("itemId").asLong();
    }

    private Response adjustStock(long productId, int delta) {
        return call(HttpMethod.PATCH, "/api/v1/products/" + productId + "/stock", venue, Map.of("delta", delta));
    }

    private int stockOf(long productId) {
        return call(HttpMethod.GET, "/api/v1/products/" + productId, venue, null).json().get("quantityInStock").asInt();
    }

    private static Map<String, Object> productBody(long brandId, String name, int quantity) {
        return Map.of("name", name, "description", "test", "quantity", quantity, "unit", "PIECE", "brandId", brandId);
    }

    private static Map<String, Object> itemBody(String name, List<?> products) {
        return Map.of("name", name, "type", "DRINK", "basePrice", 100, "minPrice", 80, "maxPrice", 150, "products", products);
    }

    private static void assertProblem(Response response, int status, String detailFragment) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.json().get("detail").asString()).contains(detailFragment);
    }
}
