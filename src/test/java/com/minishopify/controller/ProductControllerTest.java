package com.minishopify.controller;

import com.minishopify.dto.ProductForm;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void invalidProduct() throws Exception {
        when(productService.getShop(1L)).thenReturn(new Shop("Coffee Corner", "Fresh coffee"));

        mockMvc.perform(post("/shops/1/products")
                .param("name", "   ")
                .param("price", "12.50")
                .param("inventory", "-1"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/new"))
            .andExpect(model().attributeHasFieldErrors("productForm", "name", "inventory"));

        verify(productService, never()).addProduct(any(Long.class), any(ProductForm.class));
    }

    @Test
    void validProductRedirectsToProductList() throws Exception {
        Shop shop = new Shop("Coffee Corner", "Fresh coffee");
        Product product = new Product(shop, "Dark Roast", "Fresh beans", new BigDecimal("12.50"), 5);
        when(productService.addProduct(eq(1L), any(ProductForm.class))).thenReturn(product);

        mockMvc.perform(post("/shops/1/products")
                .param("name", "Dark Roast")
                .param("description", "Fresh beans")
                .param("price", "12.50")
                .param("inventory", "5"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/shops/1/products"));
    }

    @Test
    void unknownShop() throws Exception {
        when(productService.getShop(99L)).thenThrow(new NoSuchElementException("Shop not found: 99"));

        mockMvc.perform(get("/shops/99/products/new"))
            .andExpect(status().isNotFound());
    }

    @Test
    void editFormShowsCurrentProductDetails() throws Exception {
        ProductForm form = new ProductForm();
        form.setName("Dark Roast");
        when(productService.getShop(1L)).thenReturn(new Shop("Coffee Corner", "Fresh coffee"));
        when(productService.getProductForm(1L, 7L)).thenReturn(form);

        mockMvc.perform(get("/shops/1/products/7/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/edit"))
            .andExpect(model().attribute("productForm", form))
            .andExpect(model().attribute("productId", 7L));
    }

    @Test
    void validEditRedirectsToProductList() throws Exception {
        Shop shop = new Shop("Coffee Corner", "Fresh coffee");
        Product product = new Product(shop, "Light Roast", "Fresh beans", new BigDecimal("11.00"), 3);
        when(productService.updateProduct(eq(1L), eq(7L), any(ProductForm.class))).thenReturn(product);

        mockMvc.perform(post("/shops/1/products/7/edit")
                .param("name", "Light Roast")
                .param("description", "Fresh beans")
                .param("price", "11.00")
                .param("inventory", "3"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/shops/1/products"))
            .andExpect(flash().attribute("updatedProductName", "Light Roast"));
    }

    @Test
    void editToAnotherProductsNameIsRejected() throws Exception {
        when(productService.getShop(1L)).thenReturn(new Shop("Coffee Corner", "Fresh coffee"));
        when(productService.nameTakenByOtherProduct(1L, 7L, "Espresso")).thenReturn(true);

        mockMvc.perform(post("/shops/1/products/7/edit")
                .param("name", "Espresso")
                .param("price", "11.00")
                .param("inventory", "3"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/edit"))
            .andExpect(model().attributeHasFieldErrors("productForm", "name"));

        verify(productService, never()).updateProduct(any(Long.class), any(Long.class), any(ProductForm.class));
    }

    @Test
    void invalidEditReturnsFormWithValidationErrors() throws Exception {
        when(productService.getShop(1L)).thenReturn(new Shop("Coffee Corner", "Fresh coffee"));

        mockMvc.perform(post("/shops/1/products/7/edit")
                .param("name", "   ")
                .param("price", "11.00")
                .param("inventory", "-1"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/edit"))
            .andExpect(model().attributeHasFieldErrors("productForm", "name", "inventory"));

        verify(productService, never()).updateProduct(any(Long.class), any(Long.class), any(ProductForm.class));
    }

    @Test
    void productImageIsSentWithItsContentType() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        Shop shop = new Shop("Coffee Corner", "Fresh coffee");
        Product product = new Product(shop, "Dark Roast", "Fresh beans", new BigDecimal("12.50"), 5);
        product.setImage(bytes, "image/png");
        when(productService.getProductWithImage(1L, 5L)).thenReturn(product);

        mockMvc.perform(get("/shops/1/products/5/image"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(bytes));
    }

    @Test
    void missingProductImageReturnsNotFound() throws Exception {
        when(productService.getProductWithImage(1L, 5L))
            .thenThrow(new NoSuchElementException("Product has no image: 5"));

        mockMvc.perform(get("/shops/1/products/5/image"))
            .andExpect(status().isNotFound());
    }

    @Test
    void nonImageUploadReturnsFormWithImageError() throws Exception {
        when(productService.getShop(1L)).thenReturn(new Shop("Coffee Corner", "Fresh coffee"));
        MockMultipartFile notAnImage =
            new MockMultipartFile("image", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/shops/1/products")
                .file(notAnImage)
                .param("name", "Dark Roast")
                .param("price", "12.50")
                .param("inventory", "5"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/new"))
            .andExpect(model().attributeHasFieldErrors("productForm", "image"));

        verify(productService, never()).addProduct(any(Long.class), any(ProductForm.class));
    }

    @Test
    void productListShowsTheShopsProducts() throws Exception {
        Shop shop = new Shop("Coffee Corner", "Fresh coffee");
        Product product = new Product(shop, "Dark Roast", "Fresh beans", new BigDecimal("12.50"), 5);
        when(productService.getShop(1L)).thenReturn(shop);
        when(productService.getProductsForShop(1L)).thenReturn(List.of(product));

        mockMvc.perform(get("/shops/1/products"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/list"))
            .andExpect(model().attributeExists("shop", "products"));
    }

}
