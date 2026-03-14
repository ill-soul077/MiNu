package com.minuStore.MiNu.integration;

import com.minuStore.MiNu.dto.OrderItemDto;
import com.minuStore.MiNu.dto.ProductDto;
import com.minuStore.MiNu.dto.StoreDto;
import com.minuStore.MiNu.dto.UserRegistrationDto;
import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.model.Order;
import com.minuStore.MiNu.repository.*;
import com.minuStore.MiNu.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Full-stack integration tests.
 * Uses @SpringBootTest to spin up the real application context with H2 in-memory DB.
 * Each test runs inside a transaction that is rolled back after the test,
 * keeping the database clean between tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests — Full Service & Repository Stack")
class FullStackIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private StoreService storeService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ══════════════════════════════════════════════════════════
    // USER REGISTRATION & VERIFICATION FLOW
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("User Registration & Verification Flow")
    class UserRegistrationIntegrationTest {

        @Test
        @DisplayName("full flow: register → persist → verify → can create store")
        void registerAndVerifyFlow() {
            // 1. Register a seller
            UserRegistrationDto dto = UserRegistrationDto.builder()
                    .username("integration_seller")
                    .email("integration_seller@test.com")
                    .password("password123")
                    .confirmPassword("password123")
                    .role("SELLER")
                    .build();

            User saved = userService.registerUser(dto);

            // 2. Verify persisted correctly
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUsername()).isEqualTo("integration_seller");
            assertThat(saved.getRole()).isEqualTo(Role.SELLER);
            assertThat(saved.isVerified()).isFalse();

            // 3. Password is encoded
            assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();

            // 4. Admin verifies the seller
            userService.verifyUser(saved.getId());

            User verified = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(verified.isVerified()).isTrue();

            // 5. Verified seller can create a store
            Store store = storeService.createStore(
                    StoreDto.builder().name("Integration Store").description("Test").build(),
                    verified
            );
            assertThat(store.getId()).isNotNull();
            assertThat(storeRepository.existsBySellerId(verified.getId())).isTrue();
        }

        @Test
        @DisplayName("duplicate username registration should throw")
        void register_duplicateUsername_throws() {
            UserRegistrationDto dto1 = UserRegistrationDto.builder()
                    .username("dupuser").email("a@test.com")
                    .password("pass123").confirmPassword("pass123").role("CUSTOMER").build();
            UserRegistrationDto dto2 = UserRegistrationDto.builder()
                    .username("dupuser").email("b@test.com")
                    .password("pass123").confirmPassword("pass123").role("CUSTOMER").build();

            userService.registerUser(dto1);

            assertThatThrownBy(() -> userService.registerUser(dto2))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName("reject user should remove them from database")
        void rejectUser_removesFromDB() {
            UserRegistrationDto dto = UserRegistrationDto.builder()
                    .username("reject_me").email("reject@test.com")
                    .password("pass123").confirmPassword("pass123").role("CUSTOMER").build();

            User saved = userService.registerUser(dto);
            Long id = saved.getId();

            userService.rejectUser(id);

            assertThat(userRepository.findById(id)).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    // SELLER STORE & PRODUCT FLOW
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Seller Store & Product Flow")
    class SellerProductIntegrationTest {

        private User seller;
        private Store store;

        @BeforeEach
        void setUpSellerWithStore() {
            // Create and verify a seller
            seller = userRepository.save(User.builder()
                    .username("test_seller").email("test_seller@test.com")
                    .password("pass").role(Role.SELLER).verified(true).build());

            // Create a store
            store = storeService.createStore(
                    StoreDto.builder().name("Seller Store").description("Integration store").build(),
                    seller
            );
        }

        @Test
        @DisplayName("seller can create, read, update and delete a product")
        void productCRUD() {
            // CREATE
            ProductDto dto = ProductDto.builder()
                    .name("Integration Product").description("Desc")
                    .price(new BigDecimal("19.99")).stockQuantity(10)
                    .imageUrl("https://example.com/img.jpg").build();

            Product created = productService.createProduct(dto, seller);
            assertThat(created.getId()).isNotNull();
            assertThat(productRepository.existsById(created.getId())).isTrue();

            // READ
            assertThat(productService.findById(created.getId())).isPresent();
            assertThat(productService.findBySeller(seller.getId())).hasSize(1);

            // UPDATE
            ProductDto updateDto = ProductDto.builder()
                    .name("Updated Name").description("Updated Desc")
                    .price(new BigDecimal("29.99")).stockQuantity(5)
                    .imageUrl("https://example.com/updated.jpg").build();

            Product updated = productService.updateProduct(created.getId(), updateDto, seller);
            assertThat(updated.getName()).isEqualTo("Updated Name");
            assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));

            // DELETE
            productService.deleteProduct(created.getId(), seller);
            assertThat(productRepository.existsById(created.getId())).isFalse();
        }

        @Test
        @DisplayName("search by name finds product case-insensitively")
        void searchByName_caseInsensitive() {
            productRepository.save(Product.builder()
                    .name("Leather Wallet").description("Handmade")
                    .price(new BigDecimal("35.00")).stockQuantity(5)
                    .store(store).build());

            assertThat(productService.searchByName("leather")).hasSize(1);
            assertThat(productService.searchByName("LEATHER")).hasSize(1);
            assertThat(productService.searchByName("wallet")).hasSize(1);
            assertThat(productService.searchByName("xyz_nomatch")).isEmpty();
        }

        @Test
        @DisplayName("unverified seller cannot create product")
        void unverifiedSeller_cannotCreateProduct() {
            User unverified = userRepository.save(User.builder()
                    .username("unverif_seller2").email("unverif2@test.com")
                    .password("pass").role(Role.SELLER).verified(false).build());

            assertThatThrownBy(() -> productService.createProduct(TestFixtures_local.productDto(), unverified))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("verified");
        }

        @Test
        @DisplayName("seller cannot update another seller's product")
        void seller_cannotUpdateOthersSProduct() {
            Product product = productRepository.save(Product.builder()
                    .name("Owned by seller1").description("x")
                    .price(new BigDecimal("10")).stockQuantity(1)
                    .store(store).build());

            User otherSeller = userRepository.save(User.builder()
                    .username("other_seller2").email("other2@test.com")
                    .password("pass").role(Role.SELLER).verified(true).build());

            ProductDto updateDto = ProductDto.builder()
                    .name("Hacked").description("x").price(new BigDecimal("1"))
                    .stockQuantity(0).build();

            assertThatThrownBy(() -> productService.updateProduct(product.getId(), updateDto, otherSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("own products");
        }

        // Local helper to avoid static import conflict
        static class TestFixtures_local {
            static ProductDto productDto() {
                return ProductDto.builder()
                        .name("Test").description("Desc")
                        .price(new BigDecimal("10")).stockQuantity(1).build();
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // ORDER PLACEMENT FLOW
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Order Placement Flow")
    class OrderIntegrationTest {

        private User customer;
        private Product product;

        @BeforeEach
        void setUpCustomerAndProduct() {
            customer = userRepository.save(User.builder()
                    .username("order_customer").email("order_cust@test.com")
                    .password("pass").role(Role.CUSTOMER).verified(true).build());

            User seller = userRepository.save(User.builder()
                    .username("order_seller").email("order_seller@test.com")
                    .password("pass").role(Role.SELLER).verified(true).build());

            Store store = storeRepository.save(Store.builder()
                    .name("Order Store").seller(seller).build());

            product = productRepository.save(Product.builder()
                    .name("Orderable Product").description("x")
                    .price(new BigDecimal("25.00")).stockQuantity(10)
                    .store(store).build());
        }

        @Test
        @DisplayName("customer can place order and stock is reduced")
        void placeOrder_reducesStock() {
            int initialStock = product.getStockQuantity();
            List<OrderItemDto> items = List.of(
                    OrderItemDto.builder().productId(product.getId()).quantity(3).build()
            );

            Order order = orderService.createOrder(customer, items);

            // Order saved
            assertThat(order.getId()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(order.getOrderItems()).hasSize(1);

            // Stock reduced
            Product updated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStockQuantity()).isEqualTo(initialStock - 3);
        }

        @Test
        @DisplayName("order can be found by customer after creation")
        void placeOrder_canBeRetrievedByCustomer() {
            List<OrderItemDto> items = List.of(
                    OrderItemDto.builder().productId(product.getId()).quantity(1).build()
            );
            orderService.createOrder(customer, items);

            List<Order> orders = orderService.findByCustomer(customer.getId());
            assertThat(orders).hasSize(1);
            assertThat(orders.get(0).getCustomer().getId()).isEqualTo(customer.getId());
        }

        @Test
        @DisplayName("admin can update order status through all stages")
        void orderStatusProgression() {
            Order order = orderService.createOrder(customer, List.of(
                    OrderItemDto.builder().productId(product.getId()).quantity(1).build()
            ));

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

            Order paid = orderService.updateStatus(order.getId(), OrderStatus.PAID);
            assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);

            Order shipped = orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);
            assertThat(shipped.getStatus()).isEqualTo(OrderStatus.SHIPPED);

            Order delivered = orderService.updateStatus(order.getId(), OrderStatus.DELIVERED);
            assertThat(delivered.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("ordering more than available stock should throw")
        void placeOrder_exceedsStock_throws() {
            List<OrderItemDto> items = List.of(
                    OrderItemDto.builder().productId(product.getId()).quantity(999).build()
            );

            assertThatThrownBy(() -> orderService.createOrder(customer, items))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient stock");

            // Stock must NOT have been changed
            Product unchanged = productRepository.findById(product.getId()).orElseThrow();
            assertThat(unchanged.getStockQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("unverified customer cannot place order")
        void placeOrder_unverifiedCustomer_throws() {
            User unverified = userRepository.save(User.builder()
                    .username("unverif_cust2").email("unvfc2@test.com")
                    .password("pass").role(Role.CUSTOMER).verified(false).build());

            assertThatThrownBy(() -> orderService.createOrder(unverified,
                    List.of(OrderItemDto.builder().productId(product.getId()).quantity(1).build())))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("verified");
        }
    }

    // ══════════════════════════════════════════════════════════
    // STORE MANAGEMENT FLOW
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Store Management Flow")
    class StoreIntegrationTest {

        @Test
        @DisplayName("verified seller can create and update their store")
        void createAndUpdateStore() {
            User seller = userRepository.save(User.builder()
                    .username("store_seller2").email("store2@test.com")
                    .password("pass").role(Role.SELLER).verified(true).build());

            // Create
            Store store = storeService.createStore(
                    StoreDto.builder().name("My Shop").description("Original").build(), seller
            );
            assertThat(store.getId()).isNotNull();
            assertThat(storeService.sellerHasStore(seller.getId())).isTrue();

            // Update
            Store updated = storeService.updateStore(
                    StoreDto.builder().name("My Shop v2").description("Updated").build(), seller
            );
            assertThat(updated.getName()).isEqualTo("My Shop v2");
            assertThat(updated.getDescription()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("seller cannot create two stores")
        void seller_cannotHaveTwoStores() {
            User seller = userRepository.save(User.builder()
                    .username("two_store_seller").email("two@test.com")
                    .password("pass").role(Role.SELLER).verified(true).build());

            storeService.createStore(StoreDto.builder().name("First").build(), seller);

            assertThatThrownBy(() ->
                    storeService.createStore(StoreDto.builder().name("Second").build(), seller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already have a store");
        }
    }
}