package com.minishopify.dto;

import java.util.List;

/**
 * Everything the catalog page needs, loaded inside the service's transaction.
 */
public final class ShopCatalog {

    private final Long shopId;
    private final String shopName;
    private final String shopDescription;
    private final List<CatalogItem> items;

    public ShopCatalog(Long shopId, String shopName, String shopDescription, List<CatalogItem> items) {
        this.shopId = shopId;
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.items = List.copyOf(items);
    }

    public Long getShopId() {
        return shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public List<CatalogItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}