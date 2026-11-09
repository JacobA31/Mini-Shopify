package com.minishopify.service;
 
import com.minishopify.dto.CatalogItem;
import com.minishopify.dto.ShopCatalog;
import com.minishopify.exception.ShopNotFoundException;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.Comparator;
import java.util.List;

/**
 * Use case: a customer browses the product catalog of one shop.
 */
@Service
public class CatalogService {
 
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
 
    public CatalogService(ShopRepository shopRepository, ProductRepository productRepository) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
    }
 
    /**
     * Loads everything the catalog page shows for one shop, products sorted by name.
     *
     * @throws ShopNotFoundException if no shop has this id
     */
    @Transactional(readOnly = true)
    public ShopCatalog getCatalog(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(shopId));
 
        List<CatalogItem> items = productRepository.findByShopId(shopId).stream()
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CatalogService::toCatalogItem)
                .toList();
 
        return new ShopCatalog(shop.getId(), shop.getName(), shop.getDescription(), items);
    }
 
    private static CatalogItem toCatalogItem(Product product) {
        return new CatalogItem(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getInventory());
    }
}