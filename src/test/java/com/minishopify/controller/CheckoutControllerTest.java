package com.minishopify.controller;

import com.minishopify.exception.EmptyCartException;
import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.exception.OrderNotFoundException;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.CustomerOrder;
import com.minishopify.model.OrderLine;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.service.CartService;
import com.minishopify.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckoutController.class)
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CheckoutService checkoutService;
    @MockitoBean
    private CartService cartService;

    @Test
    void successfulCheckoutClearsCartAfterServiceReturnsAndRedirectsToConfirmation() throws Exception {
        CustomerOrder order = order();
        when(checkoutService.checkout()).thenReturn(order);

        mockMvc.perform(post("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/confirmation/7"));

        var calls = inOrder(checkoutService, cartService);
        calls.verify(checkoutService).checkout();
        calls.verify(cartService).clearCart();
    }

    @ParameterizedTest
    @MethodSource("checkoutErrors")
    void rejectedCheckoutShowsErrorAndPreservesCart(RuntimeException error) throws Exception {
        when(checkoutService.checkout()).thenThrow(error);

        mockMvc.perform(post("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartError", error.getMessage()));

        verifyNoInteractions(cartService);
    }

    static Stream<RuntimeException> checkoutErrors() {
        return Stream.of(new EmptyCartException(), new InsufficientInventoryException("Butter tarts", 1),
                new ProductNotFoundException(99L), new IllegalArgumentException("Invalid quantity."));
    }

    @Test
    void concurrentInventoryChangeShowsFriendlyErrorAndPreservesCart() throws Exception {
        when(checkoutService.checkout()).thenThrow(new OptimisticLockingFailureException("stale version"));

        mockMvc.perform(post("/checkout"))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartError",
                        "Inventory changed while checking out. Please review your cart and try again."));

        verifyNoInteractions(cartService);
    }

    @Test
    void confirmationDisplaysTheOrder() throws Exception {
        CustomerOrder order = order();
        when(checkoutService.getOrder(7L)).thenReturn(order);

        mockMvc.perform(get("/checkout/confirmation/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/confirmation"))
                .andExpect(model().attribute("order", order))
                .andExpect(content().string(containsString("Butter tarts")))
                .andExpect(content().string(containsString("$25.00")));
    }

    @Test
    void unknownConfirmationReturns404() throws Exception {
        when(checkoutService.getOrder(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/checkout/confirmation/99"))
                .andExpect(status().isNotFound());
    }

    private CustomerOrder order() {
        CustomerOrder order = new CustomerOrder(LocalDateTime.now());
        ReflectionTestUtils.setField(order, "id", 7L);
        Product product = new Product(new Shop("Maple Goods", null), "Butter tarts", null,
                new BigDecimal("12.50"), 5);
        order.addLine(new OrderLine(product, 2, product.getPrice()));
        return order;
    }
}
