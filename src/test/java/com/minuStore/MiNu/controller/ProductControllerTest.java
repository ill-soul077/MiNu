package com.minuStore.MiNu.controller;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.model.Product;
import com.minuStore.MiNu.model.Store;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.service.CartService;
import com.minuStore.MiNu.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;
    @MockBean private CartService cartService; // needed for GlobalControllerAdvice

    private User seller;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = TestFixtures.verifiedSeller();
        store  = TestFixtures.store(seller);
        product = TestFixtures.product(store);
        when(cartService.getItemCount()).thenReturn(0);
    }

    // ══════════════════════════════════════════════════════════
    // GET /products
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /products")
    class ListProductsTests {

        @Test
        @DisplayName("should return 200 and render products/list with all products")
        @WithAnonymousUser
        void listProducts_returnsAllProducts() throws Exception {
            when(productService.findAll()).thenReturn(List.of(product));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/list"))
                    .andExpect(model().attributeExists("products"))
                    .andExpect(model().attribute("products", hasSize(1)));
        }

        @Test
        @DisplayName("should return empty list when no products exist")
        @WithAnonymousUser
        void listProducts_emptyList() throws Exception {
            when(productService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("products", hasSize(0)));
        }

        @Test
        @DisplayName("should search products when search param provided")
        @WithAnonymousUser
        void listProducts_withSearch_callsSearchService() throws Exception {
            when(productService.searchByName("test")).thenReturn(List.of(product));

            mockMvc.perform(get("/products").param("search", "test"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("search", "test"))
                    .andExpect(model().attribute("products", hasSize(1)));

            verify(productService).searchByName("test");
            verify(productService, never()).findAll();
        }

        @Test
        @DisplayName("should call findAll when search param is blank")
        @WithAnonymousUser
        void listProducts_blankSearch_callsFindAll() throws Exception {
            when(productService.findAll()).thenReturn(List.of(product));

            mockMvc.perform(get("/products").param("search", "  "))
                    .andExpect(status().isOk());

            verify(productService).findAll();
            verify(productService, never()).searchByName(any());
        }

        @Test
        @DisplayName("should be accessible without authentication")
        @WithAnonymousUser
        void listProducts_publicAccess() throws Exception {
            when(productService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /products/{id}
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /products/{id}")
    class ProductDetailTests {

        @Test
        @DisplayName("should return 200 and render products/detail when product found")
        @WithAnonymousUser
        void productDetail_found() throws Exception {
            when(productService.findById(product.getId())).thenReturn(Optional.of(product));

            mockMvc.perform(get("/products/{id}", product.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/detail"))
                    .andExpect(model().attributeExists("product"));
        }

        @Test
        @DisplayName("should throw 500 when product not found (unhandled exception)")
        @WithAnonymousUser
        void productDetail_notFound_throws() throws Exception {
            when(productService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/products/{id}", 999L))
                    .andExpect(status().is5xxServerError());
        }
    }
}