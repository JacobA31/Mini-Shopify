package com.minishopify.repository;

import com.minishopify.model.Shop;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * Database operations for shops.
 */
public interface ShopRepository extends JpaRepository<Shop, Long> {

    @EntityGraph(attributePaths = "tags")
    List<Shop> findByNameContainingIgnoreCase(String name);

    @EntityGraph(attributePaths = "tags")
    List<Shop> findByTags_NameIgnoreCase(String tagName);

}
