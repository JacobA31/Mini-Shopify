package com.minishopify.service;

import com.minishopify.dto.ProductForm;
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
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ProductService productService;

    private ProductForm validForm() {
        ProductForm form = new ProductForm();
        form.setName("Dark Roast ");
        form.setDescription("Fresh beans");
        form.setPrice(new BigDecimal("12.50"));
        form.setInventory(5);
        return form;
    }

    @Test
    void addProduct() {
        Shop shop = new Shop("Coffee Corner", "Fresh coffee");
        when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> (Product) invocation.getArgument(0));

        Product saved = productService.addProduct(1L, validForm());

        assertEquals("Dark Roast", saved.getName());
        assertEquals("Fresh beans", saved.getDescription());
        assertEquals(0, new BigDecimal("12.50").compareTo(saved.getPrice()));
        assertEquals(5, saved.getInventory());
        assertSame(shop, saved.getShop());
        verify(productRepository).save(saved);
    }

    @Test
    void addProductToMissingShop() {
        when(shopRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> productService.addProduct(99L, validForm()));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProductsForShop() {
        Shop shop = new Shop("Coffee Corner", "Fresh coffee");
        Product product = new Product(shop, "Dark Roast", "Fresh beans", new BigDecimal("12.50"), 5);
        when(productRepository.findByShopId(1L)).thenReturn(List.of(product));

        assertEquals(List.of(product), productService.getProductsForShop(1L));
    }
}
