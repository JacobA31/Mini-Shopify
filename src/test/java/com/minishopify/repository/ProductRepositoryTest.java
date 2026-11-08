package com.minishopify.repository;

import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private EntityManager entityManager;

    private Shop coffeeShop;
    private Shop bookShop;
    private Shop emptyShop;

    @BeforeEach
    void setUp() {
        coffeeShop = shopRepository.save(new Shop("Coffee Corner", "Fresh coffee"));
        bookShop = shopRepository.save(new Shop("Book Store", "Used books"));
        emptyShop = shopRepository.save(new Shop("Empty Shop", "Nothing yet"));

        productRepository.save(new Product(coffeeShop, "Dark Roast", "Fresh beans", new BigDecimal("12.50"), 5));
        productRepository.save(new Product(coffeeShop, "Espresso Cup", "Ceramic", new BigDecimal("8.00"), 20));
        productRepository.save(new Product(bookShop, "Old Novel", "Used paperback", new BigDecimal("4.25"), 1));

        // each test reads fresh from the DB
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByShopIdReturnsEveryProduct() {
        List<String> names = productRepository.findByShopId(coffeeShop.getId()).stream()
            .map(Product::getName)
            .sorted()
            .toList();

        assertEquals(List.of("Dark Roast", "Espresso Cup"), names);
    }

    @Test
    void findByShopIdDoesNotMix() {
        List<Product> results = productRepository.findByShopId(bookShop.getId());

        assertEquals(1, results.size());
        assertEquals("Old Novel", results.get(0).getName());
    }

    @Test
    void shopWithNoProducts() {
        assertTrue(productRepository.findByShopId(emptyShop.getId()).isEmpty());
    }

    @Test
    void savedProduct() {
        Shop shop = shopRepository.findById(emptyShop.getId()).orElseThrow();
        Product saved = productRepository.save(
            new Product(shop, "Gift Card", "Digital", new BigDecimal("25.00"), 100));
        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Gift Card", found.getName());
        assertEquals("Digital", found.getDescription());
        assertEquals(0, new BigDecimal("25.00").compareTo(found.getPrice()));
        assertEquals(100, found.getInventory());
        assertEquals(emptyShop.getId(), found.getShop().getId());
    }
}
