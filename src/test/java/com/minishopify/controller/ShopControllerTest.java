package com.minishopify.controller;

import com.minishopify.dto.ShopForm;
import com.minishopify.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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

}
