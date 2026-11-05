package com.minishopify.service;

import com.minishopify.dto.ShopCatalog;
import com.minishopify.exception.ShopNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Temporary implementation until the Shop (#7) and Product (#8) entities are merged.
 * There are no shops in the database yet, so every id is "not found" (404).
 */
@Service
public class PlaceholderCatalogService implements CatalogService {

    @Override
    public ShopCatalog getCatalog(Long shopId) {
        throw new ShopNotFoundException(shopId);
    }
}