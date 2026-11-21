package com.minishopify.model;

import com.minishopify.exception.InsufficientInventoryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int inventory;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    // Product image, stored in database (null when the product has no image)
    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    // Required by JPA
    protected Product() {
    }

    public Product(Shop shop, String name, String description, BigDecimal price, int inventory) {
        this.shop = Objects.requireNonNull(shop, "shop must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.inventory = inventory;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getInventory() {
        return inventory;
    }

    public Shop getShop() {
        return shop;
    }

    public byte[] getImage() {
        return image;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public boolean hasImage() {
        return image != null && image.length > 0;
    }

    // Sets the image and its content type together
    public void setImage(byte[] image, String imageContentType) {
        this.image = Objects.requireNonNull(image, "image must not be null");
        this.imageContentType = Objects.requireNonNull(imageContentType, "imageContentType must not be null");
    }

    /** Reduces available inventory after validating the requested quantity. */
    public void decrementInventory(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        if (quantity > inventory) {
            throw new InsufficientInventoryException(name, inventory);
        }
        inventory -= quantity;
    }
}
