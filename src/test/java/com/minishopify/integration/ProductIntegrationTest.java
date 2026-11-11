package com.minishopify.integration;

import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for adding products to a shop: exercise the real HTTP routes, rendered pages,
 * service logic, and database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    private final HttpClient http = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .build();

    private HttpResponse<String> getPage(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", "text/html")
            .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postForm(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", "text/html")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String formValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // A shop for the products to belong to
    private Shop createShop() {
        return shopRepository.save(new Shop("Product Test Shop " + UUID.randomUUID(), "A shop for product tests"));
    }

    private String productForm(String name, String price, String inventory) {
        return "name=" + formValue(name)
            + "&description=" + formValue("Fresh beans")
            + "&price=" + formValue(price)
            + "&inventory=" + formValue(inventory);
    }

    @Test
    void addingProductPersistsItAndShowsItOnTheProductList() throws Exception {
        Shop shop = createShop();
        String productName = "Dark Roast " + UUID.randomUUID();

        HttpResponse<String> submission = postForm(
            "/shops/" + shop.getId() + "/products",
            productForm("  " + productName + "  ", "12.50", "5"));

        assertThat(submission.statusCode()).isBetween(300, 399);
        assertThat(URI.create(submission.headers().firstValue("Location").orElseThrow()).getPath())
            .startsWith("/shops/" + shop.getId() + "/products");

        HttpResponse<String> list = getPage("/shops/" + shop.getId() + "/products");
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body())
            .contains(shop.getName())
            .contains(productName)
            .contains("Fresh beans");

        List<Product> savedProducts = productRepository.findByShopId(shop.getId());
        assertThat(savedProducts).hasSize(1);
        assertThat(savedProducts.get(0).getName()).isEqualTo(productName);
        assertThat(savedProducts.get(0).getDescription()).isEqualTo("Fresh beans");
        assertThat(savedProducts.get(0).getPrice()).isEqualByComparingTo("12.50");
        assertThat(savedProducts.get(0).getInventory()).isEqualTo(5);
    }

    @Test
    void invalidProductIsRejectedWithoutSaving() throws Exception {
        Shop shop = createShop();

        HttpResponse<String> response = postForm(
            "/shops/" + shop.getId() + "/products",
            productForm("   ", "12.50", "-1"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Name is required");
        assertThat(productRepository.findByShopId(shop.getId())).isEmpty();
    }

    @Test
    void productOnlyAppearsInItsOwnShop() throws Exception {
        Shop shop = createShop();
        Shop otherShop = createShop();
        String productName = "Dark Roast " + UUID.randomUUID();

        HttpResponse<String> submission = postForm(
            "/shops/" + shop.getId() + "/products",
            productForm(productName, "12.50", "5"));

        assertThat(submission.statusCode()).isBetween(300, 399);

        HttpResponse<String> otherList = getPage("/shops/" + otherShop.getId() + "/products");
        assertThat(otherList.statusCode()).isEqualTo(200);
        assertThat(otherList.body()).doesNotContain(productName);
        assertThat(productRepository.findByShopId(otherShop.getId())).isEmpty();
    }

    @Test
    void unknownShopReturnsNotFound() throws Exception {
        assertThat(getPage("/shops/999999999/products").statusCode()).isEqualTo(404);
        assertThat(getPage("/shops/999999999/products/new").statusCode()).isEqualTo(404);
        assertThat(postForm("/shops/999999999/products", productForm("Dark Roast", "12.50", "5")).statusCode())
            .isEqualTo(404);
    }

}
