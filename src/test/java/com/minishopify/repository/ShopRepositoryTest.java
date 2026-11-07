package com.minishopify.repository;

import com.minishopify.model.Shop;
import com.minishopify.model.Tag;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the search queries against an in-memory H2 database.
 */
@DataJpaTest
class ShopRepositoryTest {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Tag coffee = tagRepository.save(new Tag("coffee"));
        Tag books = tagRepository.save(new Tag("books"));

        Shop cornerStore = new Shop("Corner Store", "A small local shop");
        cornerStore.addTag(coffee);
        cornerStore.addTag(books);
        shopRepository.save(cornerStore);

        shopRepository.save(new Shop("Book Store", "Used books"));

        // each test reads fresh from the DB
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void partialNameFindsShop() {
        List<Shop> results = shopRepository.findByNameContainingIgnoreCase("corn");

        assertEquals(1, results.size());
        assertEquals("Corner Store", results.get(0).getName());
    }

    @Test
    void nameSearchIgnoresCase() {
        List<Shop> results = shopRepository.findByNameContainingIgnoreCase("CORNER");

        assertEquals(1, results.size());
        assertEquals("Corner Store", results.get(0).getName());
    }

    @Test
    void tagSearchIgnoresCase() {
        List<Shop> results = shopRepository.findByTags_NameIgnoreCase("COFFEE");

        assertEquals(1, results.size());
        assertEquals("Corner Store", results.get(0).getName());
    }

    @Test
    void tagSearchReturnsEveryShopWithThatTag() {
        Tag coffee = tagRepository.findByName("coffee").orElseThrow();
        Shop beanShop = new Shop("Bean Shop", "Espresso drinks");
        beanShop.addTag(coffee);
        shopRepository.save(beanShop);
        entityManager.flush();
        entityManager.clear();

        List<String> names = shopRepository.findByTags_NameIgnoreCase("coffee").stream()
                .map(Shop::getName)
                .sorted()
                .toList();

        assertEquals(List.of("Bean Shop", "Corner Store"), names);
    }

    @Test
    void partialTagDoesNotMatch() {
        assertTrue(shopRepository.findByTags_NameIgnoreCase("cof").isEmpty());
    }

}
