package com.minishopify.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

// A category or keyword that can be shared by multiple shops.
@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_tags_name", columnNames = "name")
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // Required by JPA
    protected Tag() {
    }

    public Tag(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
