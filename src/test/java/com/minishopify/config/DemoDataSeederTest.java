package com.minishopify.config;

import com.minishopify.dto.ProductForm;
import com.minishopify.dto.ShopForm;
import com.minishopify.model.Shop;
import com.minishopify.service.ProductService;
import com.minishopify.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the seeder's rules, with the services mocked.
 */
class DemoDataSeederTest {

    private ShopService shopService;
    private ProductService productService;
    private DemoDataSeeder seeder;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        productService = mock(ProductService.class);
        seeder = new DemoDataSeeder(shopService, productService);
    }

    @Test
    void doesNothingWhenShopsAlreadyExist() {
        when(shopService.hasShops()).thenReturn(true);

        seeder.run(null);

        verify(shopService, never()).createShop(any());
        verify(productService, never()).addProduct(anyLong(), any());
    }

    @Test
    void createsEveryDemoShopAndProductWhenDatabaseIsEmpty() {
        when(shopService.hasShops()).thenReturn(false);
        Shop savedShop = mock(Shop.class);
        when(savedShop.getId()).thenReturn(42L);
        when(shopService.createShop(any())).thenReturn(savedShop);

        seeder.run(null);

        int productCount = DemoDataSeeder.DEMO_SHOPS.stream().mapToInt(shop -> shop.products().size()).sum();
        ArgumentCaptor<ShopForm> shops = ArgumentCaptor.forClass(ShopForm.class);
        ArgumentCaptor<ProductForm> products = ArgumentCaptor.forClass(ProductForm.class);
        verify(shopService, times(DemoDataSeeder.DEMO_SHOPS.size())).createShop(shops.capture());
        verify(productService, times(productCount)).addProduct(eq(42L), products.capture());

        assertThat(shops.getAllValues()).allSatisfy(form -> {
            assertThat(form.getName()).isNotBlank();
            assertThat(form.getTags()).isNotEmpty();
        });
        assertThat(products.getAllValues()).allSatisfy(form -> {
            assertThat(form.getName()).isNotBlank();
            assertThat(form.getPrice()).isPositive();
            assertThat(form.getInventory()).isNotNegative();
        });
    }

    @Test
    void demoDataHasAnOutOfStockProductForTheInventoryDemo() {
        assertThat(DemoDataSeeder.DEMO_SHOPS)
                .flatMap(DemoDataSeeder.DemoShop::products)
                .anySatisfy(product -> assertThat(product.inventory()).isZero());
    }

    @Test
    void demoDataFitsTheDatabaseColumns() {
        // shops.name/description, products.name/description are varchar(255); tags.name is varchar(100)
        assertThat(DemoDataSeeder.DEMO_SHOPS).allSatisfy(shop -> {
            assertThat(shop.name()).hasSizeLessThanOrEqualTo(255);
            assertThat(shop.description()).hasSizeLessThanOrEqualTo(255);
            assertThat(shop.tags()).allSatisfy(tag -> assertThat(tag).hasSizeLessThanOrEqualTo(100));
            assertThat(shop.products()).allSatisfy(product -> {
                assertThat(product.name()).hasSizeLessThanOrEqualTo(100);
                assertThat(product.description()).hasSizeLessThanOrEqualTo(255);
            });
        });
    }
}
