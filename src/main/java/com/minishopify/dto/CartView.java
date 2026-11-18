package com.minishopify.dto;

import java.math.BigDecimal;
import java.util.List;

/** Everything the cart page needs, loaded inside the service's transaction. */
public final class CartView {

    private final List<CartLine> lines;

    public CartView(List<CartLine> lines) {
        this.lines = List.copyOf(lines);
    }

    public List<CartLine> getLines() {
        return lines;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int getTotalQuantity() {
        return lines.stream().mapToInt(CartLine::getQuantity).sum();
    }

    public BigDecimal getTotal() {
        return lines.stream().map(CartLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}