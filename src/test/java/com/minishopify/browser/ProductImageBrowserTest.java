package com.minishopify.browser;

import com.minishopify.model.Shop;
import com.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser test: drives a real Chrome through the merchant flow of adding a product with a picture
 * and then removing the picture while editing it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductImageBrowserTest {

    private static final By LIST_IMAGE = By.cssSelector("table img");
    private static final By EDIT_LINK = By.linkText("Edit");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopRepository shopRepository;

    @TempDir
    Path tempDir;

    private WebDriver driver;
    private WebDriverWait wait;
    private Shop shop;

    @BeforeEach
    void startBrowserAndCreateShop() {
        ChromeOptions options = new ChromeOptions();
        if (!"false".equals(System.getProperty("selenium.headless"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,900", "--no-sandbox", "--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        shop = shopRepository.save(new Shop("Browser Test Shop " + UUID.randomUUID(), "Created for the Selenium test"));
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void merchantCanRemoveAProductsPictureByEditingIt() throws IOException {
        ProductFormPage form = new ProductFormPage(driver, wait, "http://localhost:" + port);

        // add a product with a picture
        form.openAddForm(shop.getId())
            .fillIn("Dark Roast", "Fresh beans", "12.50", "5")
            .chooseImage(createPicture())
            .save();

        wait.until(ExpectedConditions.visibilityOfElementLocated(LIST_IMAGE));

        wait.until(ExpectedConditions.elementToBeClickable(EDIT_LINK)).click();
        form.removeCurrentImage().save();

        wait.until(ExpectedConditions.elementToBeClickable(EDIT_LINK));
        assertThat(driver.findElements(LIST_IMAGE)).isEmpty();

        driver.get("http://localhost:" + port + "/shops/" + shop.getId() + "/catalog");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));
        assertThat(driver.findElement(By.cssSelector(".product-card h3")).getText()).isEqualTo("Dark Roast");
        assertThat(driver.findElements(By.cssSelector(".product-card img"))).isEmpty();
    }

    private Path createPicture() throws IOException {
        byte[] png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        return Files.write(tempDir.resolve("beans.png"), png);
    }

}
