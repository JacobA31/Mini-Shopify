package com.minishopify.controller;

import com.minishopify.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Customer-facing product catalog of a shop. */
@Controller
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/shops/{shopId}/catalog")
    public String catalog(@PathVariable Long shopId, Model model) {
        model.addAttribute("catalog", catalogService.getCatalog(shopId));
        return "shops/catalog";
    }
}