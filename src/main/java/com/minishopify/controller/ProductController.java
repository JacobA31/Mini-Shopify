package com.minishopify.controller;

import com.minishopify.dto.ProductForm;
import com.minishopify.model.Product;
import com.minishopify.service.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.NoSuchElementException;

// Web pages and form submissions for adding products to a shop
@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/shops/{shopId}/products")
    public String showProducts(@PathVariable Long shopId, Model model) {
        model.addAttribute("shop", productService.getShop(shopId));
        model.addAttribute("products", productService.getProductsForShop(shopId));
        return "products/list";
    }

    @GetMapping("/shops/{shopId}/products/new")
    public String showAddForm(@PathVariable Long shopId, Model model) {
        model.addAttribute("shop", productService.getShop(shopId));
        model.addAttribute("productForm", new ProductForm());
        return "products/new";
    }

    @PostMapping("/shops/{shopId}/products")
    public String addProduct(
        @PathVariable Long shopId,
        @Valid @ModelAttribute("productForm") ProductForm productForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("shop", productService.getShop(shopId));
            return "products/new";
        }

        Product addedProduct = productService.addProduct(shopId, productForm);
        redirectAttributes.addFlashAttribute("addedProductName", addedProduct.getName());
        return "redirect:/shops/" + shopId + "/products";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public void handleShopNotFound(NoSuchElementException exception, HttpServletResponse response)
        throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
    }

}
