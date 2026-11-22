package com.minishopify.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minishopify.dto.ProductForm;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;
import com.minishopify.repository.ShopRepository;
import org.springframework.web.multipart.MultipartFile;

// Handles product related work
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    public ProductService(ProductRepository productRepository, ShopRepository shopRepository) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
    }

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

        MultipartFile image = form.getImage();
        if (image != null && !image.isEmpty()) {
            try {
                product.setImage(image.getBytes(), image.getContentType().toLowerCase(Locale.ROOT));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read the uploaded image", e);
            }
        }

        return productRepository.save(product);
    }

    // Returns all products that belong to the shop
    @Transactional(readOnly = true)
    public List<Product> getProductsForShop(Long shopId) {
        return productRepository.findByShopId(shopId);
    }

    @Transactional(readOnly = true)
    public boolean nameTakenInShop(Long shopId, String name) {
        return productRepository.existsByShopIdAndNameIgnoreCase(shopId, name.trim());
    }

    @Transactional(readOnly = true)
    public boolean nameTakenByOtherProduct(Long shopId, Long productId, String name) {
        return productRepository.existsByShopIdAndNameIgnoreCaseAndIdNot(shopId, name.trim(), productId);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long shopId, Long productId) {
        return productRepository.findById(productId)
            .filter(product -> product.getShop().getId().equals(shopId))
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public ProductForm getProductForm(Long shopId, Long productId) {
        return toForm(getProduct(shopId, productId));
    }

    @Transactional
    public Product updateProduct(Long shopId, Long productId, ProductForm form) {
        Product product = getProduct(shopId, productId);
        product.update(
            form.getName().trim(),
            form.getDescription(),
            form.getPrice(),
            form.getInventory());
        return product;
    }

    // Copies a product's info into a form
    private static ProductForm toForm(Product product) {
        ProductForm form = new ProductForm();
        form.setName(product.getName());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setInventory(product.getInventory());
        return form;
    }

    @Transactional(readOnly = true)
    public Product getProductWithImage(Long shopId, Long productId) {
        Product product = productRepository.findByIdAndShopId(productId, shopId)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        if (!product.hasImage()) {
            throw new NoSuchElementException("Product has no image: " + productId);
        }

        return product;
    }

    @Transactional(readOnly = true)
    public Product getProductWithImage(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        if (!product.hasImage()) {
            throw new NoSuchElementException("Product has no image: " + productId);
        }

        return product;
    }
}
