package com.minishopify.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Read-only view of one product in a shop's catalog.
 */
public final class CatalogItem {

    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final int inventory;

    public CatalogItem(Long id, String name, String description, BigDecimal price, int inventory) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.price = Objects.requireNonNull(price, "price");
        this.inventory = inventory;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getInventory() {
        return inventory;
    }

    public boolean isInStock() {
        return inventory > 0;
    }
}