package com.minuStore.MiNu.controller;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.dto.OrderItemDto;
import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.service.CartService;
import com.minuStore.MiNu.service.OrderService;
import com.minuStore.MiNu.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private OrderService orderService;
    @MockBean private CartService cartService;
    @MockBean private ProductService productService;

    private User customer;
    private Store store;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.verifiedCustomer();
        store    = TestFixtures.store(TestFixtures.verifiedSeller());
        product  = TestFixtures.product(store);
        order    = TestFixtures.pendingOrder(customer, product);
        when(cartService.getItemCount()).thenReturn(0);
    }

    // ══════════════════════════════════════════════════════════
    // Security checks
    // ══════════════════════════════════════════════════════════
    @Test
    @DisplayName("GET /cart should require CUSTOMER role")
    void cart_deniesNonCustomer() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection()); // redirects to login
    }

    @Test
    @DisplayName("GET /orders should require CUSTOMER role")
    void orders_deniesAnonymous() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection());
    }

    // ══════════════════════════════════════════════════════════
    // Cart
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cart Operations")
    class CartTests {

        @Test
        @DisplayName("GET /cart renders cart view with items and total")
        @WithMockUser(roles = "CUSTOMER")
        void viewCart_rendersCorrectly() throws Exception {
            OrderItemDto cartItem = TestFixtures.orderItemDto(product.getId(), 2);
            when(cartService.getItems()).thenReturn(List.of(cartItem));
            when(productService.findById(product.getId())).thenReturn(Optional.of(product));
            when(cartService.getItemCount()).thenReturn(2);

            mockMvc.perform(get("/cart"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("orders/cart"))
                    .andExpect(model().attributeExists("cartItems", "total", "itemCount"))
                    .andExpect(model().attribute("itemCount", 2));
        }

        @Test
        @DisplayName("GET /cart renders empty cart when no items")
        @WithMockUser(roles = "CUSTOMER")
        void viewCart_empty() throws Exception {
            when(cartService.getItems()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/cart"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("cartItems", hasSize(0)))
                    .andExpect(model().attribute("total", BigDecimal.ZERO));
        }

        @Test
        @DisplayName("POST /cart/add adds product and redirects to /products")
        @WithMockUser(roles = "CUSTOMER")
        void addToCart_success() throws Exception {
            doNothing().when(cartService).addItem(product.getId(), 1);

            mockMvc.perform(post("/cart/add")
                            .param("productId", String.valueOf(product.getId()))
                            .param("quantity", "1")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/products"))
                    .andExpect(flash().attribute("message", containsString("added")));

            verify(cartService).addItem(product.getId(), 1);
        }

        @Test
        @DisplayName("POST /cart/add uses default quantity of 1 when not provided")
        @WithMockUser(roles = "CUSTOMER")
        void addToCart_defaultQuantity() throws Exception {
            mockMvc.perform(post("/cart/add")
                            .param("productId", String.valueOf(product.getId()))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());

            verify(cartService).addItem(product.getId(), 1);
        }

        @Test
        @DisplayName("POST /cart/remove removes item and redirects to /cart")
        @WithMockUser(roles = "CUSTOMER")
        void removeFromCart_success() throws Exception {
            doNothing().when(cartService).removeItem(product.getId());

            mockMvc.perform(post("/cart/remove")
                            .param("productId", String.valueOf(product.getId()))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cart"));

            verify(cartService).removeItem(product.getId());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Checkout
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Checkout")
    class CheckoutTests {

        @Test
        @DisplayName("POST /orders/checkout should place order and redirect to /orders")
        void checkout_success() throws Exception {
            List<OrderItemDto> items = List.of(TestFixtures.orderItemDto(product.getId(), 1));
            when(cartService.getItems()).thenReturn(items);
            when(orderService.createOrder(any(), any())).thenReturn(order);

            mockMvc.perform(post("/orders/checkout")
                            .with(csrf())
                            .with(user(customer)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders"))
                    .andExpect(flash().attribute("message", containsString("successfully")));

            verify(cartService).clear();
        }

        @Test
        @DisplayName("POST /orders/checkout should flash error and redirect to /cart on failure")
        void checkout_failure_redirectsToCart() throws Exception {
            when(cartService.getItems()).thenReturn(Collections.emptyList());
            when(orderService.createOrder(any(), any()))
                    .thenThrow(new RuntimeException("Order must contain at least one item"));

            mockMvc.perform(post("/orders/checkout")
                            .with(csrf())
                            .with(user(customer)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cart"))
                    .andExpect(flash().attributeExists("error"));

            verify(cartService, never()).clear();
        }
    }

    // ══════════════════════════════════════════════════════════
    // My Orders
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /orders")
    class MyOrdersTests {

        @Test
        @DisplayName("should render orders list for authenticated customer")
        void myOrders_rendersWithOrders() throws Exception {
            when(orderService.findByCustomer(customer.getId())).thenReturn(List.of(order));

            mockMvc.perform(get("/orders").with(user(customer)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("orders/list"))
                    .andExpect(model().attribute("orders", hasSize(1)));
        }

        @Test
        @DisplayName("should render empty orders list when customer has no orders")
        void myOrders_empty() throws Exception {
            when(orderService.findByCustomer(customer.getId())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/orders").with(user(customer)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("orders", hasSize(0)));
        }
    }
}