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
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for a merchant editing a shop's products (#24): exercise the real HTTP routes,
 * rendered pages, service logic, and database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EditProductIntegrationTest {

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

    private Shop createShop() {
        return shopRepository.save(new Shop("Edit Product Test Shop " + UUID.randomUUID(), "A shop for edit tests"));
    }

    private Product createProduct(Shop shop, String name) {
        return productRepository.save(new Product(shop, name, "Fresh beans", new BigDecimal("12.50"), 5));
    }

    private String productForm(String name, String price, String inventory) {
        return "name=" + formValue(name)
            + "&description=" + formValue("Edited description")
            + "&price=" + formValue(price)
            + "&inventory=" + formValue(inventory);
    }

    private String editPath(Shop shop, Product product) {
        return "/shops/" + shop.getId() + "/products/" + product.getId() + "/edit";
    }

    @Test
    void editingProductSavesItAndCustomersSeeTheChange() throws Exception {
        Shop shop = createShop();
        Product product = createProduct(shop, "Dark Roast");
        String newName = "Light Roast " + UUID.randomUUID();

        HttpResponse<String> productsPage = getPage("/shops/" + shop.getId() + "/products");
        assertThat(productsPage.body()).contains(editPath(shop, product));

        HttpResponse<String> editForm = getPage(editPath(shop, product));
        assertThat(editForm.statusCode()).isEqualTo(200);
        assertThat(editForm.body()).contains("Dark Roast").contains("12.50");

        HttpResponse<String> submission = postForm(editPath(shop, product), productForm("  " + newName + "  ", "9.99", "0"));

        assertThat(submission.statusCode()).isBetween(300, 399);
        assertThat(URI.create(submission.headers().firstValue("Location").orElseThrow()).getPath())
            .startsWith("/shops/" + shop.getId() + "/products");

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo(newName);
        assertThat(saved.getDescription()).isEqualTo("Edited description");
        assertThat(saved.getPrice()).isEqualByComparingTo("9.99");
        assertThat(saved.getInventory()).isZero();

        HttpResponse<String> catalog = getPage("/shops/" + shop.getId() + "/catalog");
        assertThat(catalog.body()).contains(newName).contains("Out of stock").doesNotContain("Dark Roast");
    }

    @Test
    void productCanKeepItsNameButNotTakeAnothers() throws Exception {
        Shop shop = createShop();
        Product product = createProduct(shop, "Dark Roast");
        createProduct(shop, "Espresso");

        HttpResponse<String> keepName = postForm(editPath(shop, product), productForm("dark roast", "13.00", "5"));
        assertThat(keepName.statusCode()).isBetween(300, 399);

        HttpResponse<String> takeName = postForm(editPath(shop, product), productForm("ESPRESSO", "13.00", "5"));
        assertThat(takeName.statusCode()).isEqualTo(200);
        assertThat(takeName.body()).contains("This shop already has a product with that name");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getName()).isEqualTo("dark roast");
    }

    @Test
    void invalidEditIsRejectedWithoutSaving() throws Exception {
        Shop shop = createShop();
        Product product = createProduct(shop, "Dark Roast");

        HttpResponse<String> response = postForm(editPath(shop, product), productForm("   ", "12.50", "-1"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Name is required");
        Product unchanged = productRepository.findById(product.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Dark Roast");
        assertThat(unchanged.getInventory()).isEqualTo(5);
    }

}
