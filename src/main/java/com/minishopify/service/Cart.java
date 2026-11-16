package com.minishopify.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * The current visitor's shopping cart: product ids and quantities, kept in the HTTP session
 * (one cart per browser session), not in the database. Only a completed checkout is persisted.
 *
 * <p>Holds no business rules: {@link CartService} checks products and inventory before changing it.
 * Checkout reads {@link #getItems()} and calls {@link #clear()} once the order is saved.
 */
@Component
@SessionScope
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Product id to quantity, in the order products were first added. */
    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public int getQuantity(Long productId) {
        return items.getOrDefault(productId, 0);
    }

    /** Sets a product's quantity; zero or less removes it. */
    public void setQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            items.remove(productId);
        } else {
            items.put(productId, quantity);
        }
    }

    public void remove(Long productId) {
        items.remove(productId);
    }

    public void clear() {
        items.clear();
    }

    /** Read-only view of product id to quantity. */
    public Map<Long, Integer> getItems() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Total number of units across all products. */
    public int getTotalQuantity() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }
}