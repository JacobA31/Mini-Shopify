package com.minishopify.integration;

import com.minishopify.model.Shop;
import com.minishopify.repository.ShopRepository;
import com.minishopify.repository.TagRepository;
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
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for creating a shop (#7): exercise the real HTTP routes, rendered pages,
 * service logic, and database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShopCreationIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TagRepository tagRepository;

    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();

    private HttpResponse<String> getPage(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html")
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postForm(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/shops"))
                .header("Accept", "text/html")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String formValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Test
    void creatingShopPersistsNormalizedTagsAndShowsConfirmation() throws Exception {
        String uniqueId = UUID.randomUUID().toString();
        String shopName = "Integration Shop " + uniqueId;
        String normalizedTag = "integration-" + uniqueId;
        String submittedTag = normalizedTag.toUpperCase(Locale.ROOT);
        String description = "A shop created through the web form";

        String body = "name=" + formValue(shopName)
                + "&description=" + formValue(description)
                + "&tags=" + formValue(" " + submittedTag + " ")
                + "&tags=" + formValue(normalizedTag)
                + "&tags=" + formValue("   ");

        HttpResponse<String> submission = postForm(body);

        assertThat(submission.statusCode()).isBetween(300, 399);
        assertThat(URI.create(submission.headers().firstValue("Location").orElseThrow()).getPath())
                .startsWith("/shops/created");

        HttpResponse<String> confirmation = getPage("/shops/created");
        assertThat(confirmation.statusCode()).isEqualTo(200);
        assertThat(confirmation.body())
                .contains(shopName)
                .contains(description)
                .contains(normalizedTag);

        List<Shop> savedShops = shopRepository.findByNameContainingIgnoreCase(shopName);
        assertThat(savedShops).hasSize(1);
        assertThat(savedShops.get(0).getTags())
                .extracting(tag -> tag.getName())
                .containsExactly(normalizedTag);
        assertThat(tagRepository.findByName(normalizedTag)).isPresent();
    }

    @Test
    void creatingShopWithoutTagsPersistsAndShowsEmptyTagMessage() throws Exception {
        String shopName = "No Tags Shop " + UUID.randomUUID();
        String body = "name=" + formValue(shopName)
                + "&description=" + formValue("A shop without tags");

        HttpResponse<String> submission = postForm(body);

        assertThat(submission.statusCode()).isBetween(300, 399);
        assertThat(URI.create(submission.headers().firstValue("Location").orElseThrow()).getPath())
                .startsWith("/shops/created");

        HttpResponse<String> confirmation = getPage("/shops/created");
        assertThat(confirmation.statusCode()).isEqualTo(200);
        assertThat(confirmation.body())
                .contains(shopName)
                .contains("No tags were added.");

        List<Shop> savedShops = shopRepository.findByNameContainingIgnoreCase(shopName);
        assertThat(savedShops).hasSize(1);
        assertThat(savedShops.get(0).getTags()).isEmpty();
    }

    @Test
    void blankShopNameIsRejectedWithoutSaving() throws Exception {
        long shopCountBefore = shopRepository.count();

        HttpResponse<String> response = postForm(
                "name=" + formValue("   ")
                        + "&description=" + formValue("This shop should not be saved"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Shop name is required");
        assertThat(shopRepository.count()).isEqualTo(shopCountBefore);
    }

}
