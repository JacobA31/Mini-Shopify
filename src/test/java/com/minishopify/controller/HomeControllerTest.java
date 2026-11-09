package com.minishopify.controller;

import com.minishopify.model.Shop;
import com.minishopify.model.Tag;
import com.minishopify.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Example web-layer test: {@code @WebMvcTest} starts only the MVC layer for one controller,
 * so it is fast. Services the controller needs are replaced with {@code @MockitoBean}.
 */
@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShopService shopService;

    @Test
    void homePageRendersIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void searchRendersMatchingShopsWithTags() throws Exception {
        Shop shop = new Shop("Coffee Shop", "Fresh coffee");
        shop.addTag(new Tag("espresso"));
        when(shopService.hasShops()).thenReturn(true);
        when(shopService.searchShops("coffee")).thenReturn(List.of(shop));

        mockMvc.perform(get("/").param("query", "coffee"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Coffee Shop")))
                .andExpect(content().string(containsString("Fresh coffee")))
                .andExpect(content().string(containsString("espresso")));
    }

    @Test
    void searchWithNoMatchesShowsEmptyState() throws Exception {
        when(shopService.hasShops()).thenReturn(true);
        when(shopService.searchShops("nothing")).thenReturn(List.of());

        mockMvc.perform(get("/").param("query", "nothing"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No shops found.")))
                .andExpect(content().string(not(containsString("Create a shop to get started."))));
    }
}
