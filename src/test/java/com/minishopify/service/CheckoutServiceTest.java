package com.minishopify.service;

import com.minishopify.exception.EmptyCartException;
import com.minishopify.exception.InsufficientInventoryException;
import com.minishopify.exception.OrderNotFoundException;
import com.minishopify.exception.ProductNotFoundException;
import com.minishopify.model.CustomerOrder;
import com.minishopify.model.OrderLine;
import com.minishopify.model.Product;
import com.minishopify.model.Shop;
import com.minishopify.repository.CustomerOrderRepository;
import com.minishopify.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Business rules with a real cart and mocked repositories; transactions are tested separately. */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerOrderRepository orderRepository;

    private Cart cart;
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        service = new CheckoutService(cart, productRepository, orderRepository);
    }

    @Test
    void checkoutDecrementsInventoryAndSavesQuantitiesPricesAndTotal() {
        Product tarts = product(10L, "Butter tarts", "12.50", 5);
        Product syrup = product(11L, "Maple syrup", "9.99", 1);
        cart.setQuantity(10L, 2);
        cart.setQuantity(11L, 1);
        when(productRepository.findAllById(any())).thenReturn(List.of(tarts, syrup));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));

        CustomerOrder order = service.checkout();

        assertThat(tarts.getInventory()).isEqualTo(3);
        assertThat(syrup.getInventory()).isZero();
        assertThat(order.getPlacedAt()).isNotNull();
        assertThat(order.getTotal()).isEqualByComparingTo("34.99");
        assertThat(order.getLines()).extracting(OrderLine::getQuantity).containsExactly(2, 1);
        OrderLine firstLine = order.getLines().get(0);
        assertThat(firstLine.getProduct()).isSameAs(tarts);
        assertThat(firstLine.getCustomerOrder()).isSameAs(order);
        assertThat(firstLine.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(firstLine.getLineTotal()).isEqualByComparingTo("25.00");
        verify(orderRepository).save(order);
        // The controller clears the cart only after the transaction commits.
        assertThat(cart.getTotalQuantity()).isEqualTo(3);
    }

    @Test
    void emptyCartIsRejectedWithoutAccessingRepositories() {
        assertThatThrownBy(service::checkout).isInstanceOf(EmptyCartException.class);

        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    void insufficientStockLeavesEveryProductAndTheCartUnchanged() {
        Product tarts = product(10L, "Butter tarts", "12.50", 5);
        Product syrup = product(11L, "Maple syrup", "9.99", 1);
        cart.setQuantity(10L, 2);
        cart.setQuantity(11L, 2);
        when(productRepository.findAllById(any())).thenReturn(List.of(tarts, syrup));

        assertThatThrownBy(service::checkout).isInstanceOf(InsufficientInventoryException.class);

        assertThat(tarts.getInventory()).isEqualTo(5);
        assertThat(syrup.getInventory()).isEqualTo(1);
        assertThat(cart.getQuantity(10L)).isEqualTo(2);
        assertThat(cart.getQuantity(11L)).isEqualTo(2);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void deletedProductIsRejectedBeforeAnyInventoryChanges() {
        Product tarts = product(10L, "Butter tarts", "12.50", 5);
        cart.setQuantity(10L, 2);
        cart.setQuantity(99L, 1);
        when(productRepository.findAllById(any())).thenReturn(List.of(tarts));

        assertThatThrownBy(service::checkout).isInstanceOf(ProductNotFoundException.class);

        assertThat(tarts.getInventory()).isEqualTo(5);
        assertThat(cart.getQuantity(99L)).isEqualTo(1);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrderReturnsSavedOrder() {
        CustomerOrder order = new CustomerOrder(java.time.LocalDateTime.now());
        when(orderRepository.findDetailedById(7L)).thenReturn(Optional.of(order));

        assertThat(service.getOrder(7L)).isSameAs(order);
    }

    @Test
    void unknownOrderIsRejected() {
        when(orderRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(99L)).isInstanceOf(OrderNotFoundException.class);
    }

    private Product product(Long id, String name, String price, int inventory) {
        Product product = new Product(new Shop("Maple Goods", null), name, null,
                new BigDecimal(price), inventory);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
