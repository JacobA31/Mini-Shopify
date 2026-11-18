package com.minishopify.repository;

import com.minishopify.model.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Database operations for completed customer orders. */
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"lines", "lines.product"})
    Optional<CustomerOrder> findDetailedById(Long id);

}
