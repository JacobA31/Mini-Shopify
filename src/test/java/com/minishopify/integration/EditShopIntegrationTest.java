package com.minishopify.integration;

import com.minishopify.dto.ShopForm;
import com.minishopify.model.Shop;
import com.minishopify.repository.ShopRepository;
import com.minishopify.service.ShopService;
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
 * Integration tests for a merchant editing a shop (#24): exercise the real HTTP routes, rendered pages,
 * service logic, and database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EditShopIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopRepository shopRepository;

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

    // A shop with tags, created the same way the "Create a shop" form does it
    private Shop createShop(String... tags) {
        ShopForm form = new ShopForm();
        form.setName("Edit Test Shop " + UUID.randomUUID());
        form.setDescription("Before editing");
        form.setTags(List.of(tags));
        return shopService.createShop(form);
    }

    private List<Long> idsOfShopsTagged(String tag) {
        return shopRepository.findByTags_NameIgnoreCase(tag).stream().map(Shop::getId).toList();
    }

    @Test
    void editingShopSavesNewDetailsAndReplacesTags() throws Exception {
        String oldTag = "old-" + UUID.randomUUID();
        String keptTag = "kept-" + UUID.randomUUID();
        String newTag = "new-" + UUID.randomUUID();
        Shop shop = createShop(oldTag, keptTag);
        String newName = "Renamed Shop " + UUID.randomUUID();

        HttpResponse<String> editForm = getPage("/shops/" + shop.getId() + "/edit");
        assertThat(editForm.statusCode()).isEqualTo(200);
        assertThat(editForm.body()).contains(shop.getName()).contains(oldTag).contains(keptTag);

        HttpResponse<String> submission = postForm("/shops/" + shop.getId() + "/edit",
            "name=" + formValue("  " + newName + "  ")
                + "&description=" + formValue("After editing")
                + "&tags=" + formValue(keptTag)
                + "&tags=" + formValue(newTag.toUpperCase()));

        assertThat(submission.statusCode()).isBetween(300, 399);
        URI updatedPageUri = URI.create(submission.headers().firstValue("Location").orElseThrow());
        assertThat(updatedPageUri.getPath()).startsWith("/shops/" + shop.getId() + "/updated");

        HttpResponse<String> updatedPage = getPage(updatedPageUri.getRawPath());
        assertThat(updatedPage.statusCode()).isEqualTo(200);
        assertThat(updatedPage.body())
            .contains("Shop updated")
            .contains(newName)
            .contains("After editing")
            .contains(keptTag)
            .contains(newTag)
            .doesNotContain(oldTag);

        Shop saved = shopRepository.findById(shop.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo(newName);
        assertThat(saved.getDescription()).isEqualTo("After editing");
        assertThat(idsOfShopsTagged(keptTag)).containsExactly(shop.getId());
        assertThat(idsOfShopsTagged(newTag)).containsExactly(shop.getId());
        assertThat(idsOfShopsTagged(oldTag)).doesNotContain(shop.getId());
    }

    @Test
    void renamedShopIsFoundBySearchUnderItsNewName() throws Exception {
        Shop shop = createShop();
        String newName = "Searchable " + UUID.randomUUID();

        postForm("/shops/" + shop.getId() + "/edit", "name=" + formValue(newName));

        HttpResponse<String> search = getPage("/?query=" + formValue(newName));
        assertThat(search.body()).contains(newName);
    }

    @Test
    void blankShopNameIsRejectedWithoutSaving() throws Exception {
        Shop shop = createShop();

        HttpResponse<String> response = postForm("/shops/" + shop.getId() + "/edit", "name=" + formValue("   "));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Shop name is required");
        assertThat(shopRepository.findById(shop.getId()).orElseThrow().getName()).isEqualTo(shop.getName());
    }

}
