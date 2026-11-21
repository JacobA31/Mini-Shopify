package com.minishopify.controller;

import com.minishopify.exception.EmptyCartException;
import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.CustomerOrder;
import com.minishopify.service.CartService;
import com.minishopify.service.CheckoutService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Handles checkout submission and the resulting order confirmation page. */
@Controller
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CartService cartService;

    public CheckoutController(CheckoutService checkoutService, CartService cartService) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
    }

    @PostMapping("/checkout")
    public String checkout(RedirectAttributes redirectAttributes) {
        try {
            CustomerOrder order = checkoutService.checkout();
            // The service transaction has committed before it returns to this controller.
            cartService.clearCart();
            return "redirect:/checkout/confirmation/" + order.getId();
        } catch (EmptyCartException | InsufficientInventoryException | ProductNotFoundException e) {
            redirectAttributes.addFlashAttribute("cartError", e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("cartError",
                    "Inventory changed while checking out. Please review your cart and try again.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("cartError", e.getMessage());
        }
        return "redirect:/cart";
    }

    @GetMapping("/checkout/confirmation/{orderId}")
    public String confirmation(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", checkoutService.getOrder(orderId));
        return "checkout/confirmation";
    }

}
