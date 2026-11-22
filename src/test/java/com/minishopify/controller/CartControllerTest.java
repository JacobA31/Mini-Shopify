package com.minishopify.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.minishopify.dto.CartLine;
import com.minishopify.dto.CartView;
import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.service.CartService;

/** Web-layer test: only the MVC layer for CartController, with the service mocked. */
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void cartPageShowsLinesAndTotal() throws Exception {
        when(cartService.getCart()).thenReturn(new CartView(List.of(
                new CartLine(10L, "Butter tarts", 1L, "Maple Goods", new BigDecimal("12.50"), 2, 5, false),
                new CartLine(11L, "Maple syrup", 1L, "Maple Goods", new BigDecimal("1000.00"), 1, 3, false))));

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(content().string(allOf(
                        containsString("Butter tarts"),
                        containsString("Maple Goods"),
                        containsString("$25.00"),
                        containsString("$1,025.00"),
                        containsString("Total (3 items)"))));
    }

    @Test
    void emptyCartShowsMessage() throws Exception {
        when(cartService.getCart()).thenReturn(new CartView(List.of()));

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your cart is empty.")));
    }

    @Test
    void addRedirectsBackToCatalogWithMessage() throws Exception {
        when(cartService.addToCart(10L, 2)).thenReturn("Butter tarts");

        mockMvc.perform(post("/cart/add").param("productId", "10").param("quantity", "2").param("shopId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shops/1/catalog"))
                .andExpect(flash().attribute("cartMessage", "Added 2 × Butter tarts to your cart."));
    }

    @Test
    void addDefaultsToQuantityOne() throws Exception {
        when(cartService.addToCart(10L, 1)).thenReturn("Butter tarts");

        mockMvc.perform(post("/cart/add").param("productId", "10").param("shopId", "1"))
                .andExpect(redirectedUrl("/shops/1/catalog"));
        verify(cartService).addToCart(10L, 1);
    }

    @Test
    void addAboveInventoryShowsError() throws Exception {
        when(cartService.addToCart(10L, 9)).thenThrow(new InsufficientInventoryException("Butter tarts", 5));

        mockMvc.perform(post("/cart/add").param("productId", "10").param("quantity", "9").param("shopId", "1"))
                .andExpect(redirectedUrl("/shops/1/catalog"))
                .andExpect(flash().attribute("cartError", "Only 5 of Butter tarts in stock."));
    }

    @Test
    void addUnknownProductReturns404() throws Exception {
        when(cartService.addToCart(99L, 1)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(post("/cart/add").param("productId", "99").param("shopId", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRedirectsToCart() throws Exception {
        mockMvc.perform(post("/cart/update").param("productId", "10").param("quantity", "3"))
                .andExpect(redirectedUrl("/cart"));
        verify(cartService).updateQuantity(10L, 3);
    }

    @Test
    void updateAboveInventoryShowsErrorOnCartPage() throws Exception {
        doThrow(new InsufficientInventoryException("Butter tarts", 5)).when(cartService).updateQuantity(10L, 9);

        mockMvc.perform(post("/cart/update").param("productId", "10").param("quantity", "9"))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartError", "Only 5 of Butter tarts in stock."));
    }

    @Test
    void removeRedirectsToCart() throws Exception {
        mockMvc.perform(post("/cart/remove").param("productId", "10"))
                .andExpect(redirectedUrl("/cart"));
        verify(cartService).removeFromCart(10L);
    }
}
