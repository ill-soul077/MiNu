package com.minuStore.MiNu.repository;

import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.model.Order;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository-layer integration tests using @DataJpaTest.
 * Loads only JPA context with H2 — fast and isolated.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Repository Integration Tests")
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    // ── Helpers ───────────────────────────────────────────────

    private User savedUser(String username, String email, Role role, boolean verified) {
        return userRepository.save(User.builder()
                .username(username).email(email)
                .password("encoded").role(role).verified(verified).build());
    }

    private Store savedStore(User seller, String name) {
        return storeRepository.save(Store.builder()
                .name(name).seller(seller).build());
    }

    private Product savedProduct(Store store, String name, BigDecimal price, int stock) {
        return productRepository.save(Product.builder()
                .name(name).price(price).stockQuantity(stock).store(store).build());
    }

    // ══════════════════════════════════════════════════════════
    // UserRepository
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("UserRepository")
    class UserRepositoryTests {

        @Test
        @DisplayName("findByUsername returns user when exists")
        void findByUsername_found() {
            savedUser("alice", "alice@test.com", Role.CUSTOMER, true);
            assertThat(userRepository.findByUsername("alice")).isPresent();
        }

        @Test
        @DisplayName("findByUsername returns empty when not exists")
        void findByUsername_notFound() {
            assertThat(userRepository.findByUsername("nobody")).isEmpty();
        }

        @Test
        @DisplayName("findByEmail returns user when exists")
        void findByEmail_found() {
            savedUser("bob", "bob@test.com", Role.SELLER, false);
            assertThat(userRepository.findByEmail("bob@test.com")).isPresent();
        }

        @Test
        @DisplayName("existsByUsername returns true when username taken")
        void existsByUsername_true() {
            savedUser("carol", "carol@test.com", Role.CUSTOMER, true);
            assertThat(userRepository.existsByUsername("carol")).isTrue();
        }

        @Test
        @DisplayName("existsByUsername returns false when username free")
        void existsByUsername_false() {
            assertThat(userRepository.existsByUsername("nobody")).isFalse();
        }

        @Test
        @DisplayName("existsByEmail returns true when email taken")
        void existsByEmail_true() {
            savedUser("dave", "dave@test.com", Role.CUSTOMER, false);
            assertThat(userRepository.existsByEmail("dave@test.com")).isTrue();
        }

        @Test
        @DisplayName("countByVerifiedFalse counts only unverified users")
        void countByVerifiedFalse() {
            savedUser("u1", "u1@t.com", Role.CUSTOMER, false);
            savedUser("u2", "u2@t.com", Role.CUSTOMER, false);
            savedUser("u3", "u3@t.com", Role.CUSTOMER, true);

            // 2 unverified (u1, u2) — note DataInitializer does NOT run in @DataJpaTest
            assertThat(userRepository.countByVerifiedFalse()).isEqualTo(2L);
        }
    }

    // ══════════════════════════════════════════════════════════
    // StoreRepository
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("StoreRepository")
    class StoreRepositoryTests {

        @Test
        @DisplayName("findBySellerId returns store for seller")
        void findBySellerId_found() {
            User seller = savedUser("seller_s1", "s1@t.com", Role.SELLER, true);
            Store store = savedStore(seller, "Shop");

            assertThat(storeRepository.findBySellerId(seller.getId())).isPresent()
                    .get().extracting(Store::getName).isEqualTo("Shop");
        }

        @Test
        @DisplayName("findBySellerId returns empty when seller has no store")
        void findBySellerId_notFound() {
            User seller = savedUser("seller_s2", "s2@t.com", Role.SELLER, true);
            assertThat(storeRepository.findBySellerId(seller.getId())).isEmpty();
        }

        @Test
        @DisplayName("existsBySellerId returns true when store exists")
        void existsBySellerId_true() {
            User seller = savedUser("seller_s3", "s3@t.com", Role.SELLER, true);
            savedStore(seller, "Exists");
            assertThat(storeRepository.existsBySellerId(seller.getId())).isTrue();
        }

        @Test
        @DisplayName("existsBySellerId returns false when no store")
        void existsBySellerId_false() {
            User seller = savedUser("seller_s4", "s4@t.com", Role.SELLER, true);
            assertThat(storeRepository.existsBySellerId(seller.getId())).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════
    // ProductRepository
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("ProductRepository")
    class ProductRepositoryTests {

        @Test
        @DisplayName("findByStoreId returns products for that store only")
        void findByStoreId() {
            User seller = savedUser("p_seller1", "ps1@t.com", Role.SELLER, true);
            Store store1 = savedStore(seller, "Store1");
            User seller2 = savedUser("p_seller2", "ps2@t.com", Role.SELLER, true);
            Store store2 = savedStore(seller2, "Store2");

            savedProduct(store1, "Product A", new BigDecimal("10"), 5);
            savedProduct(store1, "Product B", new BigDecimal("20"), 3);
            savedProduct(store2, "Product C", new BigDecimal("30"), 1);

            List<Product> store1Products = productRepository.findByStoreId(store1.getId());
            assertThat(store1Products).hasSize(2)
                    .extracting(Product::getName).containsExactlyInAnyOrder("Product A", "Product B");
        }

        @Test
        @DisplayName("findByStoreSellerId returns products for a specific seller")
        void findByStoreSellerId() {
            User seller = savedUser("p_seller3", "ps3@t.com", Role.SELLER, true);
            Store store = savedStore(seller, "My Store");
            savedProduct(store, "Item X", new BigDecimal("5"), 2);

            List<Product> results = productRepository.findByStoreSellerId(seller.getId());
            assertThat(results).hasSize(1).extracting(Product::getName).contains("Item X");
        }

        @Test
        @DisplayName("findByNameContainingIgnoreCase is case-insensitive")
        void findByNameContainingIgnoreCase_caseInsensitive() {
            User seller = savedUser("p_seller4", "ps4@t.com", Role.SELLER, true);
            Store store = savedStore(seller, "CaseStore");
            savedProduct(store, "UPPERCASE ITEM", new BigDecimal("1"), 1);
            savedProduct(store, "lowercase item", new BigDecimal("1"), 1);
            savedProduct(store, "Mixed Case Item", new BigDecimal("1"), 1);

            assertThat(productRepository.findByNameContainingIgnoreCase("item")).hasSize(3);
            assertThat(productRepository.findByNameContainingIgnoreCase("ITEM")).hasSize(3);
            assertThat(productRepository.findByNameContainingIgnoreCase("uppercase")).hasSize(1);
            assertThat(productRepository.findByNameContainingIgnoreCase("notfound")).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    // OrderRepository
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("OrderRepository")
    class OrderRepositoryTests {

        @Test
        @DisplayName("findByCustomerId returns orders for specific customer")
        void findByCustomerId() {
            User customer1 = savedUser("cust_o1", "co1@t.com", Role.CUSTOMER, true);
            User customer2 = savedUser("cust_o2", "co2@t.com", Role.CUSTOMER, true);
            User seller = savedUser("ord_seller", "ords@t.com", Role.SELLER, true);
            Store store = savedStore(seller, "Ord Store");
            Product product = savedProduct(store, "Ord Product", new BigDecimal("10"), 100);

            // Save 2 orders for customer1 and 1 for customer2
            for (int i = 0; i < 2; i++) {
                Order order = orderRepository.save(Order.builder()
                        .customer(customer1).status(OrderStatus.PENDING)
                        .totalAmount(new BigDecimal("10")).build());
                orderItemRepository.save(OrderItem.builder()
                        .order(order).product(product).quantity(1)
                        .priceAtPurchase(new BigDecimal("10")).build());
            }
            Order order2 = orderRepository.save(Order.builder()
                    .customer(customer2).status(OrderStatus.PENDING)
                    .totalAmount(new BigDecimal("10")).build());

            assertThat(orderRepository.findByCustomerId(customer1.getId())).hasSize(2);
            assertThat(orderRepository.findByCustomerId(customer2.getId())).hasSize(1);
        }

        @Test
        @DisplayName("findByStatus returns only orders matching that status")
        void findByStatus() {
            User customer = savedUser("stat_cust", "stat@t.com", Role.CUSTOMER, true);

            orderRepository.save(Order.builder().customer(customer)
                    .status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build());
            orderRepository.save(Order.builder().customer(customer)
                    .status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build());
            orderRepository.save(Order.builder().customer(customer)
                    .status(OrderStatus.DELIVERED).totalAmount(BigDecimal.TEN).build());

            assertThat(orderRepository.findByStatus(OrderStatus.PENDING)).hasSize(2);
            assertThat(orderRepository.findByStatus(OrderStatus.DELIVERED)).hasSize(1);
            assertThat(orderRepository.findByStatus(OrderStatus.CANCELLED)).isEmpty();
        }
    }
}