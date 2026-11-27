package com.minishopify.ui;

import com.minishopify.model.Shop;
import com.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser test: drives a real Chrome through the merchant flow of adding a product with a picture
 * and then removing the picture while editing it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(ProductImageBrowserTest.ScreenshotOnFailure.class)
class ProductImageBrowserTest {

    private static final String TEST_IMAGE = "/test-product.jpg";
    private static final By EDIT_LINK = By.linkText("Edit");
    private static final By CATALOG_CARD = By.cssSelector(".product-card");
    private static final By CATALOG_IMAGE = By.cssSelector(".product-card img");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ShopRepository shopRepository;

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
    void merchantCanRemoveAProductsPictureByEditingIt() throws Exception {
        ProductFormPage form = new ProductFormPage(driver, wait, baseUrl());

        form.openAddForm(shop.getId())
            .fillIn("Dark Roast", "Fresh beans", "12.50", "5")
            .chooseImage(testImage())
            .save();
        wait.until(ExpectedConditions.elementToBeClickable(EDIT_LINK));

        // the customer catalog shows the picture
        openCatalog();
        assertThat(driver.findElements(CATALOG_IMAGE)).hasSize(1);

        driver.get(baseUrl() + "/shops/" + shop.getId() + "/products");
        wait.until(ExpectedConditions.elementToBeClickable(EDIT_LINK)).click();
        form.removeCurrentImage().save();
        wait.until(ExpectedConditions.elementToBeClickable(EDIT_LINK));

        openCatalog();
        assertThat(driver.findElement(By.cssSelector(".product-card h3")).getText()).isEqualTo("Dark Roast");
        assertThat(driver.findElements(CATALOG_IMAGE)).isEmpty();
    }

    private void openCatalog() {
        driver.get(baseUrl() + "/shops/" + shop.getId() + "/catalog");
        wait.until(ExpectedConditions.visibilityOfElementLocated(CATALOG_CARD));
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private Path testImage() throws URISyntaxException {
        return Path.of(getClass().getResource(TEST_IMAGE).toURI());
    }

    static class ScreenshotOnFailure implements TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable failure) throws Throwable {
            WebDriver driver = ((ProductImageBrowserTest) context.getRequiredTestInstance()).driver;
            if (driver instanceof TakesScreenshot camera) {
                try {
                    Path folder = Path.of("target", "selenium");
                    Files.createDirectories(folder);
                    Files.write(folder.resolve(context.getRequiredTestMethod().getName() + ".jpg"),
                        camera.getScreenshotAs(OutputType.BYTES));
                } catch (IOException ignored) {

                }
            }
            throw failure;
        }
    }

}
