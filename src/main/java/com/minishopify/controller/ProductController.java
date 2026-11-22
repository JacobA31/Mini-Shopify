package com.minishopify.controller;

import com.minishopify.dto.ProductForm;
import com.minishopify.model.Product;
import com.minishopify.service.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

// Web pages and form submissions for adding and editing a shop's products
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

    @GetMapping("/shops/{shopId}/products/{productId}/edit")
    public String showEditForm(@PathVariable Long shopId, @PathVariable Long productId, Model model) {
        model.addAttribute("shop", productService.getShop(shopId));
        model.addAttribute("productForm", productService.getProductForm(shopId, productId));
        model.addAttribute("productId", productId);
        model.addAttribute("hasImage", productService.productHasImage(shopId, productId));
        return "products/edit";
    }

    @PostMapping("/shops/{shopId}/products/{productId}/edit")
    public String updateProduct(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @ModelAttribute("productForm") ProductForm productForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (!bindingResult.hasErrors()
            && productService.nameTakenByOtherProduct(shopId, productId, productForm.getName())) {
            bindingResult.rejectValue("name", "duplicate", "This shop already has a product with that name");
        }
        if (bindingResult.hasErrors()) {
            // Also gives a 404 for an unknown product instead of showing the form
            model.addAttribute("hasImage", productService.productHasImage(shopId, productId));
            model.addAttribute("shop", productService.getShop(shopId));
            model.addAttribute("productId", productId);
            return "products/edit";
        }

        Product updatedProduct = productService.updateProduct(shopId, productId, productForm);
        redirectAttributes.addFlashAttribute("updatedProductName", updatedProduct.getName());
        return "redirect:/shops/" + shopId + "/products";
    }

    // Sends the product's image back to the browser
    @GetMapping("/shops/{shopId}/products/{productId}/image")
    public ResponseEntity<byte[]> showProductImage(@PathVariable Long shopId, @PathVariable Long productId) {
        return imageResponse(productService.getProductWithImage(shopId, productId));
    }

    // Same image found by product id alone
    @GetMapping("/products/{productId}/image")
    public ResponseEntity<byte[]> showProductImageById(@PathVariable Long productId) {
        return imageResponse(productService.getProductWithImage(productId));
    }

    private ResponseEntity<byte[]> imageResponse(Product product) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(product.getImageContentType()))
            .header("X-Content-Type-Options", "nosniff")
            .body(product.getImage());
    }

    // A shop id that does not exist gives a normal 404 page
    @ExceptionHandler(NoSuchElementException.class)
    public void handleShopNotFound(NoSuchElementException exception, HttpServletResponse response)
        throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
    }

}
