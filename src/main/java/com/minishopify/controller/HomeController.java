package com.minishopify.controller;

import com.minishopify.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Home page: lists shops and searches them by name or tag
 */
@Controller
public class HomeController {

    private final ShopService shopService;

    public HomeController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "query", required = false) String query, Model model) {
        model.addAttribute("query", query);

        if (query != null && !query.isBlank()) {
            model.addAttribute("shops", shopService.searchShops(query));
        }
        return "index";
    }

}
