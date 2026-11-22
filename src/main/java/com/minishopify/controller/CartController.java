package com.minishopify.controller;

import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The customer's shopping cart: view it, add from a shop's catalog, change quantities, remove. */
@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("cart", cartService.getCart());
        return "cart/view";
    }

    /** Called from the catalog page; goes back to that shop's catalog with a message. */
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam Long shopId,
                            RedirectAttributes redirectAttributes) {
        try {
            String productName = cartService.addToCart(productId, quantity);
            redirectAttributes.addFlashAttribute("cartMessage",
                    "Added " + quantity + " × " + productName + " to your cart.");
        } catch (InsufficientInventoryException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("cartError", e.getMessage());
        }
        return "redirect:/shops/" + shopId + "/catalog";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@RequestParam Long productId,
                                 @RequestParam int quantity,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(productId, quantity);
        } catch (InsufficientInventoryException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("cartError", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId) {
        cartService.removeFromCart(productId);
        return "redirect:/cart";
    }
}