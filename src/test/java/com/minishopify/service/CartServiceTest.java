package com.minishopify.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.minishopify.dto.CartLine;
import com.minishopify.dto.CartView;
import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.ProductRepository;

/** Unit test: the repository is mocked and the cart is a plain object. */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ProductRepository productRepository;

    private Cart cart;
    private CartService cartService;
    private Shop shop;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cartService = new CartService(cart, productRepository);
        shop = new Shop("Maple Goods", null);
        ReflectionTestUtils.setField(shop, "id", 1L);
    }

    /** Entities only get ids from the database, so set them by hand here. */
    private Product product(long id, String name, String price, int inventory) {
        Product product = new Product(shop, name, null, new BigDecimal(price), inventory);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Test
    void addPutsProductInCartAndReturnsItsName() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10, "Butter tarts", "12.50", 5)));

        String name = cartService.addToCart(10L, 2);

        assertThat(name).isEqualTo("Butter tarts");
        assertThat(cart.getQuantity(10L)).isEqualTo(2);
    }

    @Test
    void addingAgainAccumulatesQuantity() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10, "Butter tarts", "12.50", 5)));

        cartService.addToCart(10L, 2);
        cartService.addToCart(10L, 3);

        assertThat(cart.getQuantity(10L)).isEqualTo(5);
    }

    @Test
    void addCannotExceedInventoryIncludingWhatIsAlreadyInCart() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10, "Butter tarts", "12.50", 5)));
        cartService.addToCart(10L, 4);

        assertThatThrownBy(() -> cartService.addToCart(10L, 2))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessage("Only 5 of Butter tarts in stock.");
        assertThat(cart.getQuantity(10L)).isEqualTo(4);
    }

    @Test
    void addOutOfStockProductFails() {
        when(productRepository.findById(11L)).thenReturn(Optional.of(product(11, "Maple syrup", "9.99", 0)));

        assertThatThrownBy(() -> cartService.addToCart(11L, 1))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessage("Maple syrup is out of stock.");
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void addRejectsQuantityBelowOne() {
        assertThatThrownBy(() -> cartService.addToCart(10L, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void addUnknownProductThrowsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(99L, 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateReplacesQuantity() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10, "Butter tarts", "12.50", 5)));
        cart.setQuantity(10L, 4);

        cartService.updateQuantity(10L, 1);

        assertThat(cart.getQuantity(10L)).isEqualTo(1);
    }

    @Test
    void updateToZeroRemovesWithoutLookingUpProduct() {
        cart.setQuantity(10L, 4);

        cartService.updateQuantity(10L, 0);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void updateAboveInventoryFailsAndKeepsOldQuantity() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10, "Butter tarts", "12.50", 5)));
        cart.setQuantity(10L, 2);

        assertThatThrownBy(() -> cartService.updateQuantity(10L, 6))
                .isInstanceOf(InsufficientInventoryException.class);
        assertThat(cart.getQuantity(10L)).isEqualTo(2);
    }

    @Test
    void removeTakesProductOut() {
        cart.setQuantity(10L, 2);

        cartService.removeFromCart(10L);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void clearCartRemovesAllProducts() {
        cart.setQuantity(10L, 2);
        cart.setQuantity(11L, 1);

        cartService.clearCart();

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.getTotalQuantity()).isZero();
    }

    @Test
    void getCartBuildsLinesAndTotals() {
        cart.setQuantity(10L, 2);
        cart.setQuantity(11L, 1);
        when(productRepository.findAllById(any())).thenReturn(List.of(
                product(10, "Butter tarts", "12.50", 5),
                product(11, "Maple syrup", "9.99", 3)));

        CartView view = cartService.getCart();

        assertThat(view.getLines()).extracting(CartLine::getProductName)
                .containsExactly("Butter tarts", "Maple syrup");
        CartLine tarts = view.getLines().get(0);
        assertThat(tarts.getLineTotal()).isEqualByComparingTo("25.00");
        assertThat(tarts.getShopName()).isEqualTo("Maple Goods");
        assertThat(tarts.getAvailable()).isEqualTo(5);
        assertThat(view.getTotal()).isEqualByComparingTo("34.99");
        assertThat(view.getTotalQuantity()).isEqualTo(3);
    }

    @Test
    void getCartDropsProductsThatNoLongerExist() {
        cart.setQuantity(10L, 2);
        cart.setQuantity(12L, 1);
        when(productRepository.findAllById(any())).thenReturn(List.of(product(10, "Butter tarts", "12.50", 5)));

        CartView view = cartService.getCart();

        assertThat(view.getLines()).hasSize(1);
        assertThat(cart.getItems()).containsOnlyKeys(10L);
    }

    @Test
    void emptyCartGivesEmptyView() {
        when(productRepository.findAllById(any())).thenReturn(List.of());

        CartView view = cartService.getCart();

        assertThat(view.isEmpty()).isTrue();
        assertThat(view.getTotal()).isEqualByComparingTo("0");
    }
}
