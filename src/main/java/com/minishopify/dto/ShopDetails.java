package com.minishopify.dto;

import java.util.List;
import java.util.Objects;

/**
 * Read-only view of a shop for the merchant's Shop details page.
 */
public final class ShopDetails {

    private final Long id;
    private final String name;
    private final String description;
    private final List<String> tags;

    public ShopDetails(Long id, String name, String description, List<String> tags) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.tags = List.copyOf(tags);
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

    public List<String> getTags() {
        return tags;
    }
}
