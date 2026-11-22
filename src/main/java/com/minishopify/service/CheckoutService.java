package com.minishopify.service;

import com.minishopify.exception.EmptyCartException;
import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.exception.OrderNotFoundException;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.CustomerOrder;
import com.minishopify.model.OrderLine;
import com.minishopify.model.Product;
import com.minishopify.repository.CustomerOrderRepository;
import com.minishopify.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Runs the simulated purchase and inventory updates as one database transaction. */
@Service
public class CheckoutService {

    private final Cart cart;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public CheckoutService(Cart cart, ProductRepository productRepository,
                           CustomerOrderRepository customerOrderRepository) {
        this.cart = cart;
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    /** Checks current stock, reduces inventory, and saves the order atomically. */
    @Transactional
    public CustomerOrder checkout() {
        Map<Long, Integer> cartItems = cart.getItems();
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : productRepository.findAllById(cartItems.keySet())) {
            productsById.put(product.getId(), product);
        }

        // Validate the whole cart before changing product inventory.
        for (Map.Entry<Long, Integer> item : cartItems.entrySet()) {
            Product product = productsById.get(item.getKey());
            if (product == null) {
                throw new ProductNotFoundException(item.getKey());
            }
            if (item.getValue() < 1) {
                throw new IllegalArgumentException("Cart quantities must be at least 1.");
            }
            if (item.getValue() > product.getInventory()) {
                throw new InsufficientInventoryException(product.getName(), product.getInventory());
            }
        }

        CustomerOrder order = new CustomerOrder(LocalDateTime.now());
        for (Map.Entry<Long, Integer> item : cartItems.entrySet()) {
            Product product = productsById.get(item.getKey());
            int quantity = item.getValue();

            product.decrementInventory(quantity);
            order.addLine(new OrderLine(product, quantity, product.getPrice()));
        }

        return customerOrderRepository.save(order);
    }

    /** Loads an order and its lines for the confirmation page. */
    @Transactional(readOnly = true)
    public CustomerOrder getOrder(Long orderId) {
        return customerOrderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

}
