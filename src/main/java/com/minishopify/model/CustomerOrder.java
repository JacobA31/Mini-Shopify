package com.minishopify.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A completed simulated purchase. */
@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime placedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<OrderLine> lines = new ArrayList<>();

    // Required by JPA
    protected CustomerOrder() {
    }

    public CustomerOrder(LocalDateTime placedAt) {
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt must not be null");
    }

    public void addLine(OrderLine line) {
        OrderLine orderLine = Objects.requireNonNull(line, "line must not be null");
        orderLine.setCustomerOrder(this);
        lines.add(orderLine);
        total = total.add(orderLine.getLineTotal());
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

}
