package com.minuStore.MiNu;

import com.minuStore.MiNu.dto.OrderItemDto;
import com.minuStore.MiNu.dto.ProductDto;
import com.minuStore.MiNu.dto.StoreDto;
import com.minuStore.MiNu.dto.UserRegistrationDto;
import com.minuStore.MiNu.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Central factory for building test fixtures.
 * Use these instead of constructing objects inline in every test.
 */
public class TestFixtures {

    // ── Users ──────────────────────────────────────────────────────────────

    public static User verifiedCustomer() {
        return User.builder()
                .id(1L)
                .username("customer1")
                .email("customer1@test.com")
                .password("encoded_pass")
                .role(Role.CUSTOMER)
                .verified(true)
                .orders(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static User unverifiedCustomer() {
        return User.builder()
                .id(2L)
                .username("unverified")
                .email("unverified@test.com")
                .password("encoded_pass")
                .role(Role.CUSTOMER)
                .verified(false)
                .orders(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static User verifiedSeller() {
        return User.builder()
                .id(3L)
                .username("seller1")
                .email("seller1@test.com")
                .password("encoded_pass")
                .role(Role.SELLER)
                .verified(true)
                .orders(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static User unverifiedSeller() {
        return User.builder()
                .id(4L)
                .username("unverified_seller")
                .email("unseller@test.com")
                .password("encoded_pass")
                .role(Role.SELLER)
                .verified(false)
                .orders(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static User adminUser() {
        return User.builder()
                .id(99L)
                .username("admin")
                .email("admin@test.com")
                .password("encoded_pass")
                .role(Role.ADMIN)
                .verified(true)
                .orders(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Stores ─────────────────────────────────────────────────────────────

    public static Store store(User seller) {
        return Store.builder()
                .id(10L)
                .name("Test Store")
                .description("A test store description")
                .seller(seller)
                .products(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── Products ───────────────────────────────────────────────────────────

    public static Product product(Store store) {
        return Product.builder()
                .id(100L)
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(10)
                .imageUrl("https://example.com/img.jpg")
                .store(store)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Product outOfStockProduct(Store store) {
        return Product.builder()
                .id(101L)
                .name("Out of Stock Product")
                .description("No stock")
                .price(new BigDecimal("9.99"))
                .stockQuantity(0)
                .store(store)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Orders ─────────────────────────────────────────────────────────────

    public static Order pendingOrder(User customer, Product product) {
        Order order = Order.builder()
                .id(1000L)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(product.getPrice())
                .orderItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(product)
                .quantity(1)
                .priceAtPurchase(product.getPrice())
                .build();

        order.getOrderItems().add(item);
        return order;
    }

    // ── DTOs ───────────────────────────────────────────────────────────────

    public static ProductDto productDto() {
        return ProductDto.builder()
                .name("New Product")
                .description("New product description")
                .price(new BigDecimal("49.99"))
                .stockQuantity(5)
                .imageUrl("https://example.com/new.jpg")
                .build();
    }

    public static StoreDto storeDto() {
        return StoreDto.builder()
                .name("My Store")
                .description("My store description")
                .build();
    }

    public static UserRegistrationDto customerRegistrationDto() {
        return UserRegistrationDto.builder()
                .username("newuser")
                .email("newuser@test.com")
                .password("password123")
                .confirmPassword("password123")
                .role("CUSTOMER")
                .build();
    }

    public static UserRegistrationDto sellerRegistrationDto() {
        return UserRegistrationDto.builder()
                .username("newseller")
                .email("newseller@test.com")
                .password("password123")
                .confirmPassword("password123")
                .role("SELLER")
                .build();
    }

    public static OrderItemDto orderItemDto(Long productId, int qty) {
        return OrderItemDto.builder()
                .productId(productId)
                .quantity(qty)
                .build();
    }
}