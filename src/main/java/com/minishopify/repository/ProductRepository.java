package com.minishopify.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minishopify.model.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByShopId(Long shopId);
    Optional<Product> findByIdAndShopId(Long id, Long shopId);

    boolean existsByShopIdAndNameIgnoreCase(Long shopId, String name);
}
