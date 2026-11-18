package com.minishopify.exception;

/**
 * Thrown when a quantity would exceed a product's available inventory, for example when adding
 * to the cart. Checkout can reuse it for the final inventory check.
 */
public class InsufficientInventoryException extends RuntimeException {

    private final String productName;
    private final int available;

    public InsufficientInventoryException(String productName, int available) {
        super(available == 0
                ? productName + " is out of stock."
                : "Only " + available + " of " + productName + " in stock.");
        this.productName = productName;
        this.available = available;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailable() {
        return available;
    }
}