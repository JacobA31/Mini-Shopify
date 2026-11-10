package com.minishopify.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: no Spring context. */
class ShopCatalogTest {

    private static CatalogItem item(String name, int inventory) {
        return new CatalogItem(1L, name, null, new BigDecimal("5.00"), inventory);
    }

    @Test
    void itemIsInStockOnlyWhenInventoryIsPositive() {
        assertThat(item("A", 3).isInStock()).isTrue();
        assertThat(item("B", 0).isInStock()).isFalse();
    }

    @Test
    void catalogWithoutItemsIsEmpty() {
        assertThat(new ShopCatalog(1L, "Shop", null, List.of()).isEmpty()).isTrue();
        assertThat(new ShopCatalog(1L, "Shop", null, List.of(item("A", 1))).isEmpty()).isFalse();
    }

    @Test
    void catalogKeepsItsOwnCopyOfTheItems() {
        List<CatalogItem> items = new ArrayList<>(List.of(item("A", 1)));
        ShopCatalog catalog = new ShopCatalog(1L, "Shop", null, items);

        items.add(item("B", 1));

        assertThat(catalog.getItems()).hasSize(1);
        assertThatThrownBy(() -> catalog.getItems().add(item("C", 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void itemRequiresNameAndPrice() {
        assertThatThrownBy(() -> new CatalogItem(1L, null, null, BigDecimal.ONE, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CatalogItem(1L, "A", null, null, 1))
                .isInstanceOf(NullPointerException.class);
    }
}