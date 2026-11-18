package com.minishopify.integration;

import com.minishopify.model.CustomerOrder;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.CustomerOrderRepository;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP sessions and database transactions, with an isolated database for checkout tests. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:checkout-tests;DB_CLOSE_DELAY=-1")
class CheckoutIntegrationTest {

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private HttpClient http;
    private Shop shop;
    private Product tarts;
    private Product syrup;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        shopRepository.deleteAll();
        http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        shop = shopRepository.save(new Shop("Checkout Goods", null));
        tarts = productRepository.save(new Product(shop, "Butter tarts", null, new BigDecimal("12.50"), 5));
        syrup = productRepository.save(new Product(shop, "Maple syrup", null, new BigDecimal("9.99"), 3));
    }

    @Test
    void checkoutSavesOrderDecrementsInventoryAndClearsSessionCart() throws Exception {
        addToCart(tarts, 2);
        addToCart(syrup, 1);

        HttpResponse<String> response = post("/checkout", "");
        assertThat(response.statusCode()).isEqualTo(302);
        String confirmationPath = response.headers().firstValue("Location").orElseThrow();
        CustomerOrder order = savedOrder(confirmationPath);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getTotal()).isEqualByComparingTo("34.99");
        assertThat(stock(tarts)).isEqualTo(3);
        assertThat(stock(syrup)).isEqualTo(2);

        HttpResponse<String> confirmation = get(confirmationPath);
        assertThat(confirmation.statusCode()).isEqualTo(200);
        assertThat(confirmation.body()).contains("Order confirmed", "Butter tarts", "Maple syrup", "$34.99");
        assertThat(get("/cart").body()).contains("Your cart is empty.");

        assertThat(redirectPath(post("/checkout", ""))).isEqualTo("/cart");
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void emptyCheckoutShowsErrorAndCreatesNoOrder() throws Exception {
        HttpResponse<String> response = post("/checkout", "");
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(redirectPath(response)).isEqualTo("/cart");
        assertThat(get("/cart").body()).contains("Your cart is empty.");
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void stockLostSinceAddingToCartRejectsWholePurchaseAndKeepsCart() throws Exception {
        addToCart(tarts, 2);
        addToCart(syrup, 2);
        // Simulate another customer buying syrup after this visitor filled their cart.
        jdbc.update("UPDATE products SET inventory = inventory - 2, version = version + 1 WHERE id = ?",
                syrup.getId());

        HttpResponse<String> response = post("/checkout", "");

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(redirectPath(response)).isEqualTo("/cart");
        assertThat(get("/cart").body()).contains("Only 1 of Maple syrup in stock.",
                "Butter tarts", "Maple syrup", "Total (4 items)");
        assertThat(stock(tarts)).isEqualTo(5);
        assertThat(stock(syrup)).isEqualTo(1);
        assertThat(orderRepository.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_lines", Long.class)).isZero();
    }

    @Test
    void orderUsesCheckoutPriceAndKeepsItAfterProductPriceChanges() throws Exception {
        addToCart(tarts, 2);
        jdbc.update("UPDATE products SET price = ? WHERE id = ?", new BigDecimal("15.00"), tarts.getId());

        HttpResponse<String> response = post("/checkout", "");
        assertThat(response.statusCode()).isEqualTo(302);
        String confirmationPath = response.headers().firstValue("Location").orElseThrow();
        jdbc.update("UPDATE products SET price = ? WHERE id = ?", new BigDecimal("99.00"), tarts.getId());

        CustomerOrder order = savedOrder(confirmationPath);
        assertThat(order.getLines().get(0).getUnitPrice()).isEqualByComparingTo("15.00");
        assertThat(order.getTotal()).isEqualByComparingTo("30.00");
        assertThat(get(confirmationPath).body()).contains("$15.00", "$30.00").doesNotContain("$99.00");
    }

    private int stock(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getInventory();
    }

    private CustomerOrder savedOrder(String confirmationPath) {
        long orderId = Long.parseLong(confirmationPath.substring(confirmationPath.lastIndexOf('/') + 1));
        return orderRepository.findDetailedById(orderId).orElseThrow();
    }

    private void addToCart(Product product, int quantity) throws Exception {
        HttpResponse<String> response = post("/cart/add", "productId=" + product.getId()
                + "&quantity=" + quantity + "&shopId=" + shop.getId());
        assertThat(response.statusCode()).isEqualTo(302);
    }

    private HttpResponse<String> get(String path) throws Exception {
        URI uri = URI.create("http://localhost:" + port).resolve(path);
        return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String redirectPath(HttpResponse<String> response) {
        // Tomcat may send an absolute redirect and append a session id before cookies are established.
        return URI.create(response.headers().firstValue("Location").orElseThrow())
                .getPath().split(";", 2)[0];
    }

    private HttpResponse<String> post(String path, String form) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
