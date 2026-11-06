package com.minishopify.repository;

import com.minishopify.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database operations for shops.
 */
public interface ShopRepository extends JpaRepository<Shop, Long> {
}
