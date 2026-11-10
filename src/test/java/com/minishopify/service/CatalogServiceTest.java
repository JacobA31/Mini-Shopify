package com.minishopify.service;

import com.minishopify.dto.CatalogItem;
import com.minishopify.dto.ShopCatalog;
import com.minishopify.exception.ShopNotFoundException;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test: repositories are mocked, so no Spring context or database is started. */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void returnsShopDetailsAndProductsSortedByName() {
        Shop shop = new Shop("Maple Goods", "Canadian treats");
        when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByShopId(1L)).thenReturn(List.of(
                new Product(shop, "maple syrup", "500 ml", new BigDecimal("9.99"), 0),
                new Product(shop, "Butter tarts", "Box of 6", new BigDecimal("12.50"), 4)));

        ShopCatalog catalog = catalogService.getCatalog(1L);

        assertThat(catalog.getShopName()).isEqualTo("Maple Goods");
        assertThat(catalog.getShopDescription()).isEqualTo("Canadian treats");
        assertThat(catalog.getItems()).extracting(CatalogItem::getName)
                .containsExactly("Butter tarts", "maple syrup");

        CatalogItem tarts = catalog.getItems().get(0);
        assertThat(tarts.getDescription()).isEqualTo("Box of 6");
        assertThat(tarts.getPrice()).isEqualByComparingTo("12.50");
        assertThat(tarts.getInventory()).isEqualTo(4);
        assertThat(tarts.isInStock()).isTrue();
        assertThat(catalog.getItems().get(1).isInStock()).isFalse();
    }

    @Test
    void shopWithoutProductsGivesEmptyCatalog() {
        when(shopRepository.findById(2L)).thenReturn(Optional.of(new Shop("Brand New Shop", null)));
        when(productRepository.findByShopId(2L)).thenReturn(List.of());

        assertThat(catalogService.getCatalog(2L).isEmpty()).isTrue();
    }

    @Test
    void unknownShopThrowsNotFound() {
        when(shopRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getCatalog(99L))
                .isInstanceOf(ShopNotFoundException.class)
                .hasMessageContaining("99");
        verify(productRepository, never()).findByShopId(anyLong());
    }
}