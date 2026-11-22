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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// Web pages and form submissions for creating and editing shops
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
        redirectAttributes.addFlashAttribute("createdShopId", createdShop.getId());
        return "redirect:/shops/created";
    }

    @GetMapping("/shops/created")
    public String showCreatedShop(Model model) {
        if (!model.containsAttribute("createdShopName")) {
            return "redirect:/shops/new";
        }
        return "shops/created";
    }

    @GetMapping("/shops/{shopId}/edit")
    public String showEditForm(@PathVariable Long shopId, Model model) {
        model.addAttribute("shopForm", shopService.getShopForm(shopId));
        model.addAttribute("shopId", shopId);
        return "shops/edit";
    }

    @PostMapping("/shops/{shopId}/edit")
    public String updateShop(
            @PathVariable Long shopId,
            @Valid @ModelAttribute("shopForm") ShopForm shopForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            shopService.getShop(shopId); // a 404 for an unknown shop not the form
            model.addAttribute("shopId", shopId);
            return "shops/edit";
        }

        boolean changed = shopService.updateShop(shopId, shopForm);
        if (changed) {
            redirectAttributes.addFlashAttribute("shopSaved", true);
        }
        return "redirect:/shops/" + shopId;
    }

    // The merchant's page for one shop: its name, description, and tags
    @GetMapping("/shops/{shopId}")
    public String showShopDetails(@PathVariable Long shopId, Model model) {
        model.addAttribute("shopDetails", shopService.getShopForm(shopId));
        model.addAttribute("shopId", shopId);
        return "shops/details";
    }

}
