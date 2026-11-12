package com.minishopify.integration;

import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for "View a shop's product catalog" (#10): starts the whole app on a real
 * embedded server (not MockMvc), so the database, Thymeleaf, and Spring Boot's error pages
 * are all exercised.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> getPage(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html")
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void catalogListsOnlyThatShopsProductsWithPriceAndStock() throws Exception {
        Shop shop = shopRepository.save(new Shop("Maple Goods", "Canadian treats"));
        productRepository.save(new Product(shop, "Maple syrup", "500 ml", new BigDecimal("1299.99"), 0));
        productRepository.save(new Product(shop, "Butter tarts", "Box of 6", new BigDecimal("12.50"), 4));
        Shop other = shopRepository.save(new Shop("Hockey Hut", null));
        productRepository.save(new Product(other, "Hockey stick", null, new BigDecimal("80.00"), 2));

        HttpResponse<String> response = getPage("/shops/" + shop.getId() + "/catalog");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("Maple Goods")
                .contains("Canadian treats")
                .contains("$12.50")
                .contains("4 in stock")
                .contains("$1,299.99")
                .contains("Out of stock")
                .doesNotContain("Hockey stick")
                .doesNotContain("This shop has no products yet.");
        assertThat(response.body().indexOf("Butter tarts"))
                .isLessThan(response.body().indexOf("Maple syrup"));
    }

    @Test
    void shopWithoutProductsShowsEmptyMessage() throws Exception {
        Shop shop = shopRepository.save(new Shop("Brand New Shop", null));

        HttpResponse<String> response = getPage("/shops/" + shop.getId() + "/catalog");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("This shop has no products yet.");
    }

    @Test
    void unknownShopShowsFriendly404Page() throws Exception {
        HttpResponse<String> response = getPage("/shops/999999/catalog");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body())
                .contains("Page not found")
                .contains("Mini-Shopify");
    }
}