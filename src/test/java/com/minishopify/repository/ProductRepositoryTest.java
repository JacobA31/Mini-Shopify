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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private Product darkRoast;

    @BeforeEach
    void setUp() {
        coffeeShop = shopRepository.save(new Shop("Coffee Corner", "Fresh coffee"));
        bookShop = shopRepository.save(new Shop("Book Store", "Used books"));
        emptyShop = shopRepository.save(new Shop("Empty Shop", "Nothing yet"));

        darkRoast = productRepository.save(new Product(coffeeShop, "Dark Roast", "Fresh beans", new BigDecimal("12.50"), 5));
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

    @Test
    void editedProductCanKeepItsOwnName() {
        assertFalse(productRepository.existsByShopIdAndNameIgnoreCaseAndIdNot(
            coffeeShop.getId(), "dark roast", darkRoast.getId()));
    }

    @Test
    void editedProductCannotTakeAnotherProductsName() {
        assertTrue(productRepository.existsByShopIdAndNameIgnoreCaseAndIdNot(
            coffeeShop.getId(), "ESPRESSO CUP", darkRoast.getId()));
    }

    @Test
    void findByIdAndShopIdReturnsTheProductOfThatShop() {
        Shop shop = shopRepository.findById(emptyShop.getId()).orElseThrow();
        Product saved = productRepository.save(
            new Product(shop, "Gift Card", "Digital", new BigDecimal("25.00"), 100));
        entityManager.flush();
        entityManager.clear();

        Optional<Product> found = productRepository.findByIdAndShopId(saved.getId(), emptyShop.getId());

        assertTrue(found.isPresent());
        assertEquals("Gift Card", found.get().getName());
    }

    @Test
    void findByIdAndShopIdDoesNotFindAnotherShopsProduct() {
        Shop shop = shopRepository.findById(emptyShop.getId()).orElseThrow();
        Product saved = productRepository.save(
            new Product(shop, "Gift Card", "Digital", new BigDecimal("25.00"), 100));
        entityManager.flush();
        entityManager.clear();

        assertTrue(productRepository.findByIdAndShopId(saved.getId(), bookShop.getId()).isEmpty());
    }

    @Test
    void savedImageKeepsItsBytesAndContentType() {
        Shop shop = shopRepository.findById(emptyShop.getId()).orElseThrow();
        // 1000 bytes on purpose: it proves the column is not cut off at the default 255
        byte[] bytes = new byte[1000];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        Product product = new Product(shop, "Gift Card", "Digital", new BigDecimal("25.00"), 100);
        product.setImage(bytes, "image/png");
        Product saved = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(saved.getId()).orElseThrow();

        assertTrue(found.hasImage());
        assertArrayEquals(bytes, found.getImage());
        assertEquals("image/png", found.getImageContentType());
    }
}
