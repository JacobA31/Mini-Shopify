package com.minishopify.service;

import com.minishopify.dto.ProductForm;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

// Handles product related work
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    public ProductService(ProductRepository productRepository, ShopRepository shopRepository) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
    }

    // Finds a shop by id, or throws if it does not exist
    @Transactional(readOnly = true)
    public Shop getShop(Long shopId) {
        return shopRepository.findById(shopId)
            .orElseThrow(() -> new NoSuchElementException("Shop not found: " + shopId));
    }

    // Creates a product from the submitted form data and adds it to the shop
    @Transactional
    public Product addProduct(Long shopId, ProductForm form) {
        Shop shop = getShop(shopId);

        Product product = new Product(
            shop,
            form.getName().trim(),
            form.getDescription(),
            form.getPrice(),
            form.getInventory());

        return productRepository.save(product);
    }

    // Returns all products that belong to the shop
    @Transactional(readOnly = true)
    public List<Product> getProductsForShop(Long shopId) {
        return productRepository.findByShopId(shopId);
    }
}
