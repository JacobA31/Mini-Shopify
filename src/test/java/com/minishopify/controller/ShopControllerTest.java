package com.minishopify.controller;

import com.minishopify.dto.ShopForm;
import com.minishopify.model.Shop;
import com.minishopify.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ShopController.class)
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShopService shopService;

    @Test
    void blankShopNameReturnsFormWithValidationError() throws Exception {
        mockMvc.perform(post("/shops")
                        .param("name", "   ")
                        .param("description", "A small local shop"))
                .andExpect(status().isOk())
                .andExpect(view().name("shops/new"))
                .andExpect(model().attributeHasFieldErrors("shopForm", "name"));

        verify(shopService, never()).createShop(any(ShopForm.class));
    }

    @Test
    void validEditRedirectsToShopDetailsWithSavedMessage() throws Exception {
        when(shopService.updateShop(eq(1L), any(ShopForm.class))).thenReturn(true);

        mockMvc.perform(post("/shops/1/edit")
                        .param("name", "Tea Shop")
                        .param("description", "leaf tea")
                        .param("tags", "tea"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shops/1"))
                .andExpect(flash().attribute("shopSaved", true));
    }

    @Test
    void savingWithoutChangesShowsNoSavedMessage() throws Exception {
        when(shopService.updateShop(eq(1L), any(ShopForm.class))).thenReturn(false);

        mockMvc.perform(post("/shops/1/edit").param("name", "Tea Shop"))
                .andExpect(redirectedUrl("/shops/1"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    void blankNameOnEditReturnsFormWithValidationError() throws Exception {
        when(shopService.getShop(1L)).thenReturn(new Shop("Corner Store", null));

        mockMvc.perform(post("/shops/1/edit").param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("shops/edit"))
                .andExpect(model().attributeHasFieldErrors("shopForm", "name"));

        verify(shopService, never()).updateShop(any(Long.class), any(ShopForm.class));
    }
}
