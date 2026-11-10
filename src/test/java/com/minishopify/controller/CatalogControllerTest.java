package com.minishopify.controller;

import com.minishopify.dto.CatalogItem;
import com.minishopify.dto.ShopCatalog;
import com.minishopify.exception.ShopNotFoundException;
import com.minishopify.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web-layer test: starts only the MVC layer (controller + Thymeleaf) with the service mocked,
 * so the catalog page can be tested before the Shop and Product entities exist.
 */
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @Test
    void catalogPagePutsCatalogInModel() throws Exception {
        ShopCatalog catalog = new ShopCatalog(1L, "Maple Goods", "Canadian treats",
                List.of(new CatalogItem(10L, "Butter tarts", "Box of 6", new BigDecimal("12.50"), 4)));
        when(catalogService.getCatalog(1L)).thenReturn(catalog);

        mockMvc.perform(get("/shops/1/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("shops/catalog"))
                .andExpect(model().attribute("catalog", catalog));
    }

    @Test
    void catalogPageShowsShopProductsPricesAndStock() throws Exception {
        when(catalogService.getCatalog(1L)).thenReturn(new ShopCatalog(1L, "Maple Goods", "Canadian treats",
                List.of(new CatalogItem(10L, "Butter tarts", "Box of 6", new BigDecimal("12.50"), 4),
                        new CatalogItem(11L, "Maple syrup", null, new BigDecimal("1299.99"), 0))));

        mockMvc.perform(get("/shops/1/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<title>Maple Goods | Mini-Shopify</title>"),
                        containsString("Canadian treats"),
                        containsString("Box of 6"),
                        containsString("$12.50"),
                        containsString("4 in stock"),
                        containsString("$1,299.99"),
                        containsString("Out of stock"),
                        not(containsString("This shop has no products yet.")))))
                .andExpect(content().string(stringContainsInOrder("Butter tarts", "Maple syrup")));
    }

    @Test
    void shopWithoutProductsShowsEmptyMessage() throws Exception {
        when(catalogService.getCatalog(2L)).thenReturn(new ShopCatalog(2L, "Brand New Shop", null, List.of()));

        mockMvc.perform(get("/shops/2/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This shop has no products yet.")))
                .andExpect(content().string(not(containsString("product-grid"))));
    }

    @Test
    void unknownShopReturns404() throws Exception {
        when(catalogService.getCatalog(99L)).thenThrow(new ShopNotFoundException(99L));

        mockMvc.perform(get("/shops/99/catalog"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonNumericShopIdIsRejected() throws Exception {
        mockMvc.perform(get("/shops/abc/catalog"))
                .andExpect(status().isBadRequest());
    }
}