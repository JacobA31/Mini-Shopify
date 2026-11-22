package com.minishopify.integration;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;

/**
 * Integration test for the shopping cart: a real server and database, and a real HTTP client
 * that keeps the session cookie, so the cart lives in the session like it does for a browser.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    /** A new client per test means a new cookie jar, so a new session and an empty cart. */
    private HttpClient http;

    private Shop shop;
    private Product tarts;
    private Product syrup;

    @BeforeEach
    void setUp() {
        http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        shop = shopRepository.save(new Shop("Maple Goods", null));
        tarts = productRepository.save(new Product(shop, "Butter tarts", null, new BigDecimal("12.50"), 3));
        syrup = productRepository.save(new Product(shop, "Maple syrup", null, new BigDecimal("9.99"), 0));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html")
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String form) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> addToCart(Product product, int quantity) throws Exception {
        return post("/cart/add", "productId=" + product.getId() + "&quantity=" + quantity
                + "&shopId=" + shop.getId());
    }

    @Test
    void addedProductsShowInCartWithTotal() throws Exception {
        HttpResponse<String> added = addToCart(tarts, 2);

        assertThat(added.statusCode()).isEqualTo(302);
        assertThat(added.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).contains("/shops/" + shop.getId() + "/catalog"));

        HttpResponse<String> cart = get("/cart");
        assertThat(cart.statusCode()).isEqualTo(200);
        assertThat(cart.body())
                .contains("Butter tarts")
                .contains("$25.00")
                .contains("Total (2 items)");
    }

    @Test
    void cartNeverHoldsMoreThanInventory() throws Exception {
        addToCart(tarts, 2);
        addToCart(tarts, 2);
        addToCart(syrup, 1);

        HttpResponse<String> cart = get("/cart");
        assertThat(cart.body())
                .contains("Total (2 items)")
                .doesNotContain("Maple syrup");
    }

    @Test
    void updateAndRemoveChangeTheCart() throws Exception {
        addToCart(tarts, 1);

        post("/cart/update", "productId=" + tarts.getId() + "&quantity=3");
        assertThat(get("/cart").body()).contains("Total (3 items)").contains("$37.50");

        post("/cart/remove", "productId=" + tarts.getId());
        assertThat(get("/cart").body()).contains("Your cart is empty.");
    }

    @Test
    void eachSessionHasItsOwnCart() throws Exception {
        addToCart(tarts, 1);

        HttpClient otherVisitor = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        HttpResponse<String> otherCart = otherVisitor.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/cart")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(otherCart.body()).contains("Your cart is empty.");
    }

    @Test
    void catalogShowsAddToCartOnlyForProductsInStock() throws Exception {
        String catalog = get("/shops/" + shop.getId() + "/catalog").body();

        assertThat(catalog).contains("Add to cart");
        assertThat(catalog.split("Add to cart", -1)).hasSize(2);
    }
}