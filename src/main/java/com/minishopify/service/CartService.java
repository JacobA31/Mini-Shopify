package com.minishopify.service;

import com.minishopify.dto.CartLine;
import com.minishopify.dto.CartView;
import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.Product;
import com.minishopify.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Use case: a customer keeps a shopping cart (add, view, change quantity, remove).
 *
 * <p>The cart can never hold more of a product than is in stock. Checkout must still re-check
 * inventory in its own transaction, because stock can change after something is added.
 */
@Service
public class CartService {

    private final Cart cart;
    private final ProductRepository productRepository;

    /** {@code cart} is a session-scoped proxy: each visitor gets their own cart. */
    public CartService(Cart cart, ProductRepository productRepository) {
        this.cart = cart;
        this.productRepository = productRepository;
    }

    /**
     * Adds {@code quantity} units of a product on top of what is already in the cart.
     *
     * @return the product's name, for the confirmation message
     * @throws IllegalArgumentException if quantity is less than 1
     * @throws ProductNotFoundException if the product doesn't exist
     * @throws InsufficientInventoryException if the cart would hold more than is in stock
     */
    @Transactional(readOnly = true)
    public String addToCart(Long productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        Product product = findProduct(productId);
        int newQuantity = cart.getQuantity(productId) + quantity;
        checkInventory(product, newQuantity);
        cart.setQuantity(productId, newQuantity);
        return product.getName();
    }

    /**
     * Replaces a product's quantity in the cart; 0 removes it.
     *
     * @throws IllegalArgumentException if quantity is negative
     * @throws ProductNotFoundException if the product doesn't exist
     * @throws InsufficientInventoryException if quantity is more than is in stock
     */
    @Transactional(readOnly = true)
    public void updateQuantity(Long productId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity can't be negative.");
        }
        if (quantity == 0) {
            cart.remove(productId);
            return;
        }
        checkInventory(findProduct(productId), quantity);
        cart.setQuantity(productId, quantity);
    }

    public void removeFromCart(Long productId) {
        cart.remove(productId);
    }

    /** Clears the current visitor's cart after a successful checkout. */
    public void clearCart() {
        cart.clear();
    }

    /**
     * Loads the cart's products with their current names, prices, shops, and stock.
     * Products that no longer exist are dropped from the cart.
     */
    @Transactional(readOnly = true)
    public CartView getCart() {
        Map<Long, Integer> items = cart.getItems();
        Map<Long, Product> products = productRepository.findAllById(items.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartLine> lines = new ArrayList<>();
        items.forEach((productId, quantity) -> {
            Product product = products.get(productId);
            if (product == null) {
                cart.remove(productId);
                return;
            }
            lines.add(new CartLine(product.getId(), product.getName(),
                    product.getShop().getId(), product.getShop().getName(),
                    product.getPrice(), quantity, product.getInventory()));
        });
        return new CartView(lines);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private static void checkInventory(Product product, int quantity) {
        if (quantity > product.getInventory()) {
            throw new InsufficientInventoryException(product.getName(), product.getInventory());
        }
    }
}
