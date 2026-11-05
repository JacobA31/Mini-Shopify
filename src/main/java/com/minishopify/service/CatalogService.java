package com.minishopify.service;

import com.minishopify.dto.ShopCatalog;
import com.minishopify.exception.ShopNotFoundException;

/**
 * Use case: a customer browses the product catalog of one shop.
 */
public interface CatalogService {

    /**
     * Loads everything the catalog page shows for one shop, products sorted by name.
     *
     * @throws ShopNotFoundException if no shop has this id
     */
    ShopCatalog getCatalog(Long shopId);
}