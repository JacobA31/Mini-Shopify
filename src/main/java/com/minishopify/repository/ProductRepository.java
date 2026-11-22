package com.minishopify.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minishopify.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByShopId(Long shopId);

    boolean existsByShopIdAndNameIgnoreCase(Long shopId, String name);

    // Same check while editing but it ignores the product being edited so keeping its own name is allowed
    boolean existsByShopIdAndNameIgnoreCaseAndIdNot(Long shopId, String name, Long id);
}
