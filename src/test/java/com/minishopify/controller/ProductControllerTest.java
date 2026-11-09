package com.minishopify.controller;

import com.minishopify.dto.ProductForm;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

}
