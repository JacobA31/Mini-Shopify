package com.minishopify.integration;

import com.minishopify.config.DemoDataSeeder;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import com.minishopify.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the demo data (#21): starts the whole app with seeding turned on and
 * checks that the demo shops show up on the real pages.
 *
 * <p>Uses its own in-memory database ({@code seeder-test}), so the demo data doesn't leak into
 * the other integration tests, which expect to start from an empty database.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "minishopify.seed.enabled=true",
                "spring.datasource.url=jdbc:h2:mem:seeder-test"
        })
class DemoDataSeederIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TagRepository tagRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> getPage(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html")
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void demoShopsAreSearchableByTagOnStartup() throws Exception {
        HttpResponse<String> response = getPage("/?query=food");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("Rideau Roasters")
                .contains("ByWard Bakehouse")
                .doesNotContain("Glebe Page Turners");
    }

    @Test
    void demoCatalogShowsInStockAndOutOfStockProducts() throws Exception {
        Shop roasters = shopRepository.findByNameContainingIgnoreCase("Rideau Roasters").get(0);

        HttpResponse<String> response = getPage("/shops/" + roasters.getId() + "/catalog");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("House blend beans")
                .contains("$16.99")
                .contains("Cold brew concentrate")
                .contains("Out of stock");
    }

    @Test
    void runningTheSeederAgainAddsNothing() {
        long shops = shopRepository.count();
        long products = productRepository.count();
        long tags = tagRepository.count();

        seeder.run(null);

        assertThat(shops).isPositive();
        assertThat(shopRepository.count()).isEqualTo(shops);
        assertThat(productRepository.count()).isEqualTo(products);
        assertThat(tagRepository.count()).isEqualTo(tags);
    }
}
