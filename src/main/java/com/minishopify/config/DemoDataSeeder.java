package com.minishopify.config;

import com.minishopify.dto.ProductForm;
import com.minishopify.dto.ShopForm;
import com.minishopify.model.Shop;
import com.minishopify.service.ProductService;
import com.minishopify.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fills an empty database with demo shops and products when the app starts, so the live app
 * always has something to search, browse, and buy.
 *
 * <p>It only runs when there are no shops at all, so it never duplicates or overwrites real data.
 * Data goes through {@link ShopService} and {@link ProductService}, so it is cleaned up
 * (trimmed names, lowercase tags) exactly like data entered through the forms.
 *
 * <p>Turned on by default and turned off in tests with {@code minishopify.seed.enabled=false}.
 */
@Component
@ConditionalOnBooleanProperty(name = "minishopify.seed.enabled", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    record DemoProduct(String name, String description, String price, int inventory) {
    }

    record DemoShop(String name, String description, List<String> tags, List<DemoProduct> products) {
    }

    static final List<DemoShop> DEMO_SHOPS = List.of(
            new DemoShop("Rideau Roasters", "Small-batch coffee, roasted in Ottawa every week.",
                    List.of("coffee", "local", "food"),
                    List.of(
                            new DemoProduct("House blend beans", "Medium roast, 340 g bag.", "16.99", 25),
                            new DemoProduct("Ethiopia single origin", "Light roast with berry notes, 340 g bag.", "19.50", 12),
                            new DemoProduct("Ceramic pour-over dripper", "Fits most mugs. Filters not included.", "34.00", 6),
                            new DemoProduct("Cold brew concentrate", "1 L bottle. Mix 1:1 with water or milk.", "14.00", 0))),
            new DemoShop("ByWard Bakehouse", "Fresh bread and pastries, baked every morning.",
                    List.of("bakery", "food", "local"),
                    List.of(
                            new DemoProduct("Sourdough loaf", "Naturally leavened, about 800 g.", "8.50", 15),
                            new DemoProduct("Butter tarts", "Box of 6, with or without raisins.", "12.00", 10),
                            new DemoProduct("Maple pecan croissant", "Our most popular pastry. Only one left!", "4.25", 1))),
            new DemoShop("Glebe Page Turners", "New and used books, with a soft spot for Canadian authors.",
                    List.of("books", "gifts"),
                    List.of(
                            new DemoProduct("Used paperback mystery", "Assorted titles, good condition.", "6.00", 30),
                            new DemoProduct("Canadian cookbook", "Hardcover, 200 recipes from coast to coast.", "29.99", 4),
                            new DemoProduct("Bookmark set", "Set of 5 illustrated bookmarks.", "5.00", 50))),
            new DemoShop("Gatineau Trail Co.", "Gear for day hikes and weekend camping trips.",
                    List.of("outdoors", "gifts"),
                    List.of(
                            new DemoProduct("Insulated water bottle", "750 ml, keeps drinks cold for 24 hours.", "32.00", 20),
                            new DemoProduct("Trail map set", "Waterproof maps of the Gatineau Park trails.", "12.50", 8),
                            new DemoProduct("Daypack", "20 L, with a rain cover.", "74.99", 3),
                            new DemoProduct("Headlamp", "Rechargeable, 300 lumens.", "24.99", 9)))
    );

    private final ShopService shopService;
    private final ProductService productService;

    public DemoDataSeeder(ShopService shopService, ProductService productService) {
        this.shopService = shopService;
        this.productService = productService;
    }

    // One transaction: if anything fails, nothing is saved, and the next startup tries again
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (shopService.hasShops()) {
            log.info("Database already has shops; skipping demo data");
            return;
        }

        for (DemoShop demoShop : DEMO_SHOPS) {
            Shop shop = shopService.createShop(toShopForm(demoShop));
            for (DemoProduct demoProduct : demoShop.products()) {
                productService.addProduct(shop.getId(), toProductForm(demoProduct));
            }
        }
        log.info("Seeded {} demo shops", DEMO_SHOPS.size());
    }

    private static ShopForm toShopForm(DemoShop demoShop) {
        ShopForm form = new ShopForm();
        form.setName(demoShop.name());
        form.setDescription(demoShop.description());
        form.setTags(demoShop.tags());
        return form;
    }

    private static ProductForm toProductForm(DemoProduct demoProduct) {
        ProductForm form = new ProductForm();
        form.setName(demoProduct.name());
        form.setDescription(demoProduct.description());
        form.setPrice(new BigDecimal(demoProduct.price()));
        form.setInventory(demoProduct.inventory());
        return form;
    }
}
