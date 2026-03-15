package com.minuStore.MiNu.service;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.dto.OrderItemDto;
import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.repository.OrderRepository;
import com.minuStore.MiNu.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User verifiedCustomer;
    private User unverifiedCustomer;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        verifiedCustomer = TestFixtures.verifiedCustomer();
        unverifiedCustomer = TestFixtures.unverifiedCustomer();
        store = TestFixtures.store(TestFixtures.verifiedSeller());
        product = TestFixtures.product(store);
    }

    // ══════════════════════════════════════════════════════════
    // createOrder
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTests {

        @Test
        @DisplayName("should create order successfully for verified customer")
        void createOrder_success() {
            // Arrange
            List<OrderItemDto> items = List.of(TestFixtures.orderItemDto(product.getId(), 2));
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Order result = orderService.createOrder(verifiedCustomer, items);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.getCustomer()).isEqualTo(verifiedCustomer);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(
                    product.getPrice().multiply(BigDecimal.valueOf(2))
            );
            assertThat(result.getOrderItems()).hasSize(1);
        }

        @Test
        @DisplayName("should deduct stock after order creation")
        void createOrder_deductsStock() {
            // Arrange
            int initialStock = product.getStockQuantity(); // 10
            List<OrderItemDto> items = List.of(TestFixtures.orderItemDto(product.getId(), 3));
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            orderService.createOrder(verifiedCustomer, items);

            // Assert — stock reduced by 3
            assertThat(product.getStockQuantity()).isEqualTo(initialStock - 3);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("should calculate correct total for multiple items")
        void createOrder_correctTotal_multipleItems() {
            // Arrange
            Product product2 = TestFixtures.product(store);
            product2.setId(200L);
            product2.setPrice(new BigDecimal("10.00"));
            product2.setStockQuantity(5);

            List<OrderItemDto> items = List.of(
                    TestFixtures.orderItemDto(product.getId(), 1),   // 29.99
                    TestFixtures.orderItemDto(product2.getId(), 2)   // 20.00
            );
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(productRepository.findById(product2.getId())).thenReturn(Optional.of(product2));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Order result = orderService.createOrder(verifiedCustomer, items);

            // Assert: 29.99 + 20.00 = 49.99
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        }

        @Test
        @DisplayName("should throw exception when customer is not verified")
        void createOrder_unverifiedCustomer_throws() {
            List<OrderItemDto> items = List.of(TestFixtures.orderItemDto(1L, 1));

            assertThatThrownBy(() -> orderService.createOrder(unverifiedCustomer, items))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("verified");

            verifyNoInteractions(productRepository, orderRepository);
        }

        @Test
        @DisplayName("should throw exception when item list is empty")
        void createOrder_emptyItems_throws() {
            assertThatThrownBy(() -> orderService.createOrder(verifiedCustomer, Collections.emptyList()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("should throw exception when item list is null")
        void createOrder_nullItems_throws() {
            assertThatThrownBy(() -> orderService.createOrder(verifiedCustomer, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void createOrder_productNotFound_throws() {
            List<OrderItemDto> items = List.of(TestFixtures.orderItemDto(999L, 1));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(verifiedCustomer, items))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("should throw exception when stock is insufficient")
        void createOrder_insufficientStock_throws() {
            product.setStockQuantity(2);
            List<OrderItemDto> items = List.of(TestFixtures.orderItemDto(product.getId(), 5));
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> orderService.createOrder(verifiedCustomer, items))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient stock");

            // Stock should NOT have changed
            assertThat(product.getStockQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("should store price-at-purchase snapshot, not current price")
        void createOrder_priceSnapshot_isStored() {
            BigDecimal originalPrice = product.getPrice(); // 29.99
            List<OrderItemDto> items = List.of(TestFixtures.orderItemDto(product.getId(), 1));
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            orderService.createOrder(verifiedCustomer, items);

            Order saved = orderCaptor.getValue();
            assertThat(saved.getOrderItems().get(0).getPriceAtPurchase())
                    .isEqualByComparingTo(originalPrice);
        }
    }

    // ══════════════════════════════════════════════════════════
    // findByCustomer / findAll / findById
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Query Methods")
    class QueryTests {

        @Test
        @DisplayName("findByCustomer returns orders for that customer")
        void findByCustomer_returnsList() {
            Order order = TestFixtures.pendingOrder(verifiedCustomer, product);
            when(orderRepository.findByCustomerId(verifiedCustomer.getId()))
                    .thenReturn(List.of(order));

            List<Order> result = orderService.findByCustomer(verifiedCustomer.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCustomer()).isEqualTo(verifiedCustomer);
        }

        @Test
        @DisplayName("findByCustomer returns empty list when no orders")
        void findByCustomer_noOrders_returnsEmpty() {
            when(orderRepository.findByCustomerId(anyLong())).thenReturn(Collections.emptyList());

            assertThat(orderService.findByCustomer(99L)).isEmpty();
        }

        @Test
        @DisplayName("findAll returns all orders")
        void findAll_returnsAll() {
            Order o1 = TestFixtures.pendingOrder(verifiedCustomer, product);
            Order o2 = TestFixtures.pendingOrder(verifiedCustomer, product);
            when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

            assertThat(orderService.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("findById returns order when found")
        void findById_found() {
            Order order = TestFixtures.pendingOrder(verifiedCustomer, product);
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

            assertThat(orderService.findById(1000L)).isEqualTo(order);
        }

        @Test
        @DisplayName("findById throws when not found")
        void findById_notFound_throws() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    // ══════════════════════════════════════════════════════════
    // updateStatus
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("should update order status successfully")
        void updateStatus_success() {
            Order order = TestFixtures.pendingOrder(verifiedCustomer, product);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should throw when order not found during update")
        void updateStatus_orderNotFound_throws() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateStatus(999L, OrderStatus.DELIVERED))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Order not found");
        }
    }
}