package com.minishopify.integration;

import com.minishopify.model.Shop;
import com.minishopify.model.Tag;
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
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for searching shops by name or tag (#9): exercise the real HTTP route,
 * rendered home page, service logic, and database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TagRepository tagRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> getPage(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html")
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Shop saveShop(String name, String description, String... tagNames) {
        Shop shop = new Shop(name, description);
        for (String tagName : tagNames) {
            shop.addTag(tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName))));
        }
        return shopRepository.save(shop);
    }

    @Test
    void searchByNameShowsMatchingShopWithCatalogLink() throws Exception {
        String uniqueId = UUID.randomUUID().toString();
        Shop coffee = saveShop("Coffee Shop " + uniqueId, "Roasted beans");
        Shop tea = saveShop("Tea Shop " + UUID.randomUUID(), "Loose leaf tea");

        HttpResponse<String> response = getPage("/?query=" + uniqueId.toUpperCase(Locale.ROOT));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains(coffee.getName())
                .contains("Roasted beans")
                .contains("href=\"/shops/" + coffee.getId() + "/catalog\"")
                .doesNotContain(tea.getName());
    }

    @Test
    void searchByTagShowsEveryShopWithThatTag() throws Exception {
        String tag = "coffee-" + UUID.randomUUID();
        Shop beanShop = saveShop("Bean Shop " + UUID.randomUUID(), null, tag);
        Shop dailyCoffee = saveShop("Daily Coffee " + UUID.randomUUID(), null, tag, "bakery");
        Shop teaHouse = saveShop("Tea House " + UUID.randomUUID(), null, "tea");

        HttpResponse<String> response = getPage("/?query=" + tag);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains(beanShop.getName())
                .contains(dailyCoffee.getName())
                .doesNotContain(teaHouse.getName());
    }

    @Test
    void searchWithNoMatchesShowsEmptyMessage() throws Exception {
        HttpResponse<String> response = getPage("/?query=nothing-" + UUID.randomUUID());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("No shops found.")
                .doesNotContain("class=\"shop-card\"");
    }

    @Test
    void clickingSearchResultOpensThatShopsCatalog() throws Exception {
        String uniqueId = UUID.randomUUID().toString();
        Shop shop = saveShop("Coffee Shop " + uniqueId, "Fresh coffee beans");
        String catalogPath = "/shops/" + shop.getId() + "/catalog";

        HttpResponse<String> results = getPage("/?query=" + uniqueId);
        assertThat(results.body()).contains("href=\"" + catalogPath + "\"");

        HttpResponse<String> catalog = getPage(catalogPath);

        assertThat(catalog.statusCode()).isEqualTo(200);
        assertThat(catalog.body())
                .contains(shop.getName())
                .contains("Fresh coffee beans");
    }

}
