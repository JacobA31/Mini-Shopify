package com.minishopify.controller;

import com.minishopify.dto.ShopForm;
import com.minishopify.model.Shop;
import com.minishopify.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// Web pages and form submissions for creating shops
@Controller
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/shops/new")
    public String showCreateForm(Model model) {
        model.addAttribute("shopForm", new ShopForm());
        return "shops/new";
    }

    @PostMapping("/shops")
    public String createShop(
            @Valid @ModelAttribute("shopForm") ShopForm shopForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "shops/new";
        }

        Shop createdShop = shopService.createShop(shopForm);
        List<String> createdTagNames = createdShop.getTags().stream()
                .map(tag -> tag.getName())
                .sorted()
                .toList();

        redirectAttributes.addFlashAttribute("createdShopName", createdShop.getName());
        redirectAttributes.addFlashAttribute("createdShopDescription", createdShop.getDescription());
        redirectAttributes.addFlashAttribute("createdShopTags", createdTagNames);
        return "redirect:/shops/created";
    }

    @GetMapping("/shops/created")
    public String showCreatedShop(Model model) {
        if (!model.containsAttribute("createdShopName")) {
            return "redirect:/shops/new";
        }
        return "shops/created";
    }

}
