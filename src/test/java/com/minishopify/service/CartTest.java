package com.minishopify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

/** Plain unit tests: no Spring context, the cart is just an object here. */
class CartTest {

    @Test
    void newCartIsEmpty() {
        Cart cart = new Cart();

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.getTotalQuantity()).isZero();
        assertThat(cart.getQuantity(1L)).isZero();
    }

    @Test
    void setQuantityAddsAndReplaces() {
        Cart cart = new Cart();

        cart.setQuantity(1L, 2);
        cart.setQuantity(2L, 3);
        cart.setQuantity(1L, 5);

        assertThat(cart.getQuantity(1L)).isEqualTo(5);
        assertThat(cart.getTotalQuantity()).isEqualTo(8);
        assertThat(cart.getItems()).containsExactly(
                org.assertj.core.api.Assertions.entry(1L, 5),
                org.assertj.core.api.Assertions.entry(2L, 3));
    }

    @Test
    void zeroQuantityRemovesProduct() {
        Cart cart = new Cart();
        cart.setQuantity(1L, 2);

        cart.setQuantity(1L, 0);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void removeAndClear() {
        Cart cart = new Cart();
        cart.setQuantity(1L, 2);
        cart.setQuantity(2L, 1);

        cart.remove(1L);
        assertThat(cart.getItems()).containsOnlyKeys(2L);

        cart.clear();
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void itemsViewCannotChangeTheCart() {
        Cart cart = new Cart();
        cart.setQuantity(1L, 2);

        assertThatThrownBy(() -> cart.getItems().put(3L, 1))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(cart.getItems()).containsOnlyKeys(1L);
    }
}