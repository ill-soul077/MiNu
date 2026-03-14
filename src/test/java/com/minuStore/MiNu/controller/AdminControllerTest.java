package com.minuStore.MiNu.controller;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private OrderService orderService;
    @MockBean private ProductService productService;
    @MockBean private StoreService storeService;
    @MockBean private CartService cartService;

    private User customer;
    private User seller;
    private Store store;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.verifiedCustomer();
        seller   = TestFixtures.verifiedSeller();
        store    = TestFixtures.store(seller);
        product  = TestFixtures.product(store);
        order    = TestFixtures.pendingOrder(customer, product);

        when(cartService.getItemCount()).thenReturn(0);
    }

    // ══════════════════════════════════════════════════════════
    // Security — Non-admin should be denied
    // ══════════════════════════════════════════════════════════
    @Test
    @DisplayName("should deny access to non-admin user")
    @WithMockUser(roles = "CUSTOMER")
    void dashboard_customerForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should deny access to unauthenticated user")
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    // ══════════════════════════════════════════════════════════
    // Dashboard
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /admin/dashboard")
    @WithMockUser(roles = "ADMIN")
    class DashboardTests {

        @Test
        @DisplayName("should render dashboard with stats")
        void dashboard_rendersWithStats() throws Exception {
            when(userService.countAll()).thenReturn(10L);
            when(userService.countUnverified()).thenReturn(2L);
            when(storeService.findAll()).thenReturn(List.of(store));
            when(orderService.findAll()).thenReturn(List.of(order));

            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/dashboard"))
                    .andExpect(model().attribute("userCount", 10L))
                    .andExpect(model().attribute("unverifiedCount", 2L))
                    .andExpect(model().attribute("storeCount", 1))
                    .andExpect(model().attribute("orderCount", 1));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Users
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("User Management")
    @WithMockUser(roles = "ADMIN")
    class UserManagementTests {

        @Test
        @DisplayName("GET /admin/users should list all users")
        void listUsers() throws Exception {
            when(userService.findAllUsers()).thenReturn(List.of(customer, seller));

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/users"))
                    .andExpect(model().attribute("users", hasSize(2)));
        }

        @Test
        @DisplayName("POST /admin/users/{id}/verify should verify user and redirect")
        void verifyUser_success() throws Exception {
            doNothing().when(userService).verifyUser(customer.getId());

            mockMvc.perform(post("/admin/users/{id}/verify", customer.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users"))
                    .andExpect(flash().attribute("message", containsString("verified")));

            verify(userService).verifyUser(customer.getId());
        }

        @Test
        @DisplayName("POST /admin/users/{id}/verify should flash error on exception")
        void verifyUser_error() throws Exception {
            doThrow(new RuntimeException("User not found")).when(userService).verifyUser(999L);

            mockMvc.perform(post("/admin/users/{id}/verify", 999L).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("POST /admin/users/{id}/reject should reject user and redirect")
        void rejectUser_success() throws Exception {
            doNothing().when(userService).rejectUser(customer.getId());

            mockMvc.perform(post("/admin/users/{id}/reject", customer.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users"))
                    .andExpect(flash().attribute("message", containsString("rejected")));
        }

        @Test
        @DisplayName("POST /admin/users/{id}/delete should delete user and redirect")
        void deleteUser_success() throws Exception {
            doNothing().when(userService).deleteUser(customer.getId());

            mockMvc.perform(post("/admin/users/{id}/delete", customer.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users"))
                    .andExpect(flash().attribute("message", containsString("deleted")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Products
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Product Management")
    @WithMockUser(roles = "ADMIN")
    class ProductManagementTests {

        @Test
        @DisplayName("GET /admin/products lists all products")
        void listProducts() throws Exception {
            when(productService.findAll()).thenReturn(List.of(product));

            mockMvc.perform(get("/admin/products"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/products"))
                    .andExpect(model().attribute("products", hasSize(1)));
        }

        @Test
        @DisplayName("POST /admin/products/{id}/delete deletes product and redirects")
        void deleteProduct_success() throws Exception {
            doNothing().when(productService).deleteProductAsAdmin(product.getId());

            mockMvc.perform(post("/admin/products/{id}/delete", product.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/products"))
                    .andExpect(flash().attribute("message", containsString("deleted")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Orders
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Order Management")
    @WithMockUser(roles = "ADMIN")
    class OrderManagementTests {

        @Test
        @DisplayName("GET /admin/orders lists all orders with status enum")
        void listOrders() throws Exception {
            when(orderService.findAll()).thenReturn(List.of(order));

            mockMvc.perform(get("/admin/orders"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/orders"))
                    .andExpect(model().attributeExists("orders", "statuses"));
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status updates status and redirects")
        void updateOrderStatus_success() throws Exception {
            when(orderService.updateStatus(order.getId(), OrderStatus.SHIPPED))
                    .thenReturn(order);

            mockMvc.perform(post("/admin/orders/{id}/status", order.getId())
                            .param("status", "SHIPPED")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/orders"))
                    .andExpect(flash().attribute("message", containsString("updated")));
        }

        @Test
        @DisplayName("POST /admin/orders/{id}/status flashes error on bad status value")
        void updateOrderStatus_invalidStatus_flashesError() throws Exception {
            mockMvc.perform(post("/admin/orders/{id}/status", order.getId())
                            .param("status", "INVALID_STATUS")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }
    }
}