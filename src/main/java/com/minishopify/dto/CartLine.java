package com.minishopify.dto;

import java.math.BigDecimal;
import java.util.Objects;

/** Read-only view of one product in the cart, with its current price and stock. */
public final class CartLine {

    private final Long productId;
    private final String productName;
    private final Long shopId;
    private final String shopName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final int available;
    private final boolean hasImage;

    public CartLine(Long productId, String productName, Long shopId, String shopName,
                    BigDecimal unitPrice, int quantity, int available, boolean hasImage) {
        this.productId = productId;
        this.productName = Objects.requireNonNull(productName, "productName");
        this.shopId = shopId;
        this.shopName = shopName;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        this.quantity = quantity;
        this.available = available;
        this.hasImage = hasImage;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Long getShopId() {
        return shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    /** Units currently in stock; the quantity field on the cart page can't go above this. */
    public int getAvailable() {
        return available;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean hasImage() { return hasImage; }
}
