package com.minuStore.MiNu.ControllerTest;

import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for AdminController.
 * Uses MockMvc + SpringBootTest + @WithMockUser(role=ADMIN).
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private ProductService productService;

    @MockBean
    private StoreService storeService;

    // ──────────────────────────────────────────────────────────────────────
    // Test 1: Unauthenticated access to /admin/dashboard → redirect to login
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("GET /admin/dashboard - unauthenticated should redirect to login")
    void getDashboard_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 2: ADMIN accesses /admin/dashboard → 200 OK
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("GET /admin/dashboard - authenticated ADMIN should return dashboard view")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getDashboard_asAdmin_shouldReturnDashboardView() throws Exception {
        when(userService.countAll()).thenReturn(5L);
        when(userService.countUnverified()).thenReturn(2L);
        when(storeService.findAll()).thenReturn(Collections.emptyList());
        when(orderService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("userCount"))
                .andExpect(model().attributeExists("unverifiedCount"))
                .andExpect(model().attributeExists("storeCount"))
                .andExpect(model().attributeExists("orderCount"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 3: ADMIN verifies a user → redirects to /admin/users
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("POST /admin/users/{id}/verify - ADMIN should verify user and redirect")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void verifyUser_asAdmin_shouldRedirectToUsers() throws Exception {
        doNothing().when(userService).verifyUser(1L);

        mockMvc.perform(post("/admin/users/1/verify")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService, times(1)).verifyUser(1L);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 4: CUSTOMER role accessing /admin/** → forbidden (403)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("GET /admin/users - CUSTOMER role should be forbidden")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getUsers_asCustomer_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 5: ADMIN lists users → returns users view with model
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("GET /admin/users - ADMIN should return users view with user list")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void listUsers_asAdmin_shouldReturnUsersView() throws Exception {
        User mockUser = User.builder()
                .id(1L)
                .username("seller1")
                .email("seller1@test.com")
                .role(Role.SELLER)
                .verified(false)
                .build();

        when(userService.findAllUsers()).thenReturn(List.of(mockUser));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"));
    }
}
