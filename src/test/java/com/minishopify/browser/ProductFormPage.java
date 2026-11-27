package com.minishopify.browser;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;

/**
 * Page Object for the add/edit product form, so the test reads as user actions
 */
class ProductFormPage {

    private static final By NAME = By.id("name");
    private static final By DESCRIPTION = By.id("description");
    private static final By PRICE = By.id("price");
    private static final By INVENTORY = By.id("inventory");
    private static final By IMAGE_FILE = By.id("image");
    private static final By SAVE = By.cssSelector("#product-form button[type='submit']");
    private static final By REMOVE_IMAGE = By.id("remove-image-button");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    ProductFormPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
    }

    ProductFormPage openAddForm(Long shopId) {
        driver.get(baseUrl + "/shops/" + shopId + "/products/new");
        wait.until(ExpectedConditions.visibilityOfElementLocated(NAME));
        return this;
    }

    ProductFormPage fillIn(String name, String description, String price, String inventory) {
        type(NAME, name);
        type(DESCRIPTION, description);
        type(PRICE, price);
        type(INVENTORY, inventory);
        return this;
    }

    // Selenium "uploads" a file by typing its path into the file input
    ProductFormPage chooseImage(Path file) {
        driver.findElement(IMAGE_FILE).sendKeys(file.toAbsolutePath().toString());
        return this;
    }

    ProductFormPage removeCurrentImage() {
        wait.until(ExpectedConditions.elementToBeClickable(REMOVE_IMAGE)).click();
        return this;
    }

    void save() {
        driver.findElement(SAVE).click();
    }

    private void type(By field, String text) {
        WebElement element = driver.findElement(field);
        element.clear();
        element.sendKeys(text);
    }
}
