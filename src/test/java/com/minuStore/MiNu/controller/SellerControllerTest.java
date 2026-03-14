package com.minuStore.MiNu.controller;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.dto.ProductDto;
import com.minuStore.MiNu.dto.StoreDto;
import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.service.CartService;
import com.minuStore.MiNu.service.ProductService;
import com.minuStore.MiNu.service.StoreService;
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
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerController.class)
@DisplayName("SellerController Tests")
class SellerControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;
    @MockBean private StoreService storeService;
    @MockBean private CartService cartService;

    private User verifiedSeller;
    private User unverifiedSeller;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        verifiedSeller   = TestFixtures.verifiedSeller();
        unverifiedSeller = TestFixtures.unverifiedSeller();
        store            = TestFixtures.store(verifiedSeller);
        product          = TestFixtures.product(store);
        when(cartService.getItemCount()).thenReturn(0);
    }

    // ══════════════════════════════════════════════════════════
    // Security
    // ══════════════════════════════════════════════════════════
    @Test
    @DisplayName("should deny CUSTOMER from accessing seller pages")
    @WithMockUser(roles = "CUSTOMER")
    void sellerPages_deniedToCustomer() throws Exception {
        mockMvc.perform(get("/seller/store"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should deny anonymous user from accessing seller pages")
    void sellerPages_deniedToAnonymous() throws Exception {
        mockMvc.perform(get("/seller/store"))
                .andExpect(status().is3xxRedirection());
    }

    // ══════════════════════════════════════════════════════════
    // Store Management
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Store Management")
    class StoreManagementTests {

        @Test
        @DisplayName("GET /seller/store shows create-store form when seller has no store")
        void viewStore_noStore_showsCreateForm() throws Exception {
            when(storeService.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.empty());

            mockMvc.perform(get("/seller/store").with(user(verifiedSeller)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("seller/store"))
                    .andExpect(model().attribute("hasStore", false))
                    .andExpect(model().attributeExists("storeDto"));
        }

        @Test
        @DisplayName("GET /seller/store shows existing store when seller has one")
        void viewStore_withStore_showsStore() throws Exception {
            when(storeService.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.of(store));

            mockMvc.perform(get("/seller/store").with(user(verifiedSeller)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("seller/store"))
                    .andExpect(model().attribute("hasStore", true))
                    .andExpect(model().attributeExists("store"));
        }

        @Test
        @DisplayName("GET /seller/store redirects unverified seller to store-form error page")
        void viewStore_unverifiedSeller_showsError() throws Exception {
            mockMvc.perform(get("/seller/store").with(user(unverifiedSeller)))
                    .andExpect(view().name("seller/store-form"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("POST /seller/store creates store and redirects")
        void createStore_success() throws Exception {
            when(storeService.createStore(any(StoreDto.class), eq(verifiedSeller))).thenReturn(store);

            mockMvc.perform(post("/seller/store")
                            .with(user(verifiedSeller))
                            .with(csrf())
                            .param("name", "My New Store")
                            .param("description", "A great store"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/seller/store"))
                    .andExpect(flash().attribute("message", containsString("created")));
        }

        @Test
        @DisplayName("POST /seller/store flashes error when service throws")
        void createStore_serviceError() throws Exception {
            when(storeService.createStore(any(), any()))
                    .thenThrow(new RuntimeException("You already have a store"));

            mockMvc.perform(post("/seller/store")
                            .with(user(verifiedSeller))
                            .with(csrf())
                            .param("name", "Duplicate Store")
                            .param("description", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @DisplayName("POST /seller/store/update updates store and redirects")
        void updateStore_success() throws Exception {
            when(storeService.updateStore(any(StoreDto.class), eq(verifiedSeller))).thenReturn(store);

            mockMvc.perform(post("/seller/store/update")
                            .with(user(verifiedSeller))
                            .with(csrf())
                            .param("name", "Updated Store Name")
                            .param("description", "Updated description"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/seller/store"))
                    .andExpect(flash().attribute("message", containsString("updated")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Product Management
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Product Management")
    class ProductManagementTests {

        @Test
        @DisplayName("GET /seller/products lists seller's products")
        void myProducts_listsProducts() throws Exception {
            when(productService.findBySeller(verifiedSeller.getId())).thenReturn(List.of(product));
            when(storeService.sellerHasStore(verifiedSeller.getId())).thenReturn(true);

            mockMvc.perform(get("/seller/products").with(user(verifiedSeller)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("seller/products"))
                    .andExpect(model().attribute("products", hasSize(1)))
                    .andExpect(model().attribute("hasStore", true));
        }

        @Test
        @DisplayName("GET /seller/products shows hasStore=false when seller has no store")
        void myProducts_noStore_flagIsCorrect() throws Exception {
            when(productService.findBySeller(verifiedSeller.getId())).thenReturn(Collections.emptyList());
            when(storeService.sellerHasStore(verifiedSeller.getId())).thenReturn(false);

            mockMvc.perform(get("/seller/products").with(user(verifiedSeller)))
                    .andExpect(model().attribute("hasStore", false));
        }

        @Test
        @DisplayName("GET /seller/products/new shows product form for verified seller with store")
        void newProductForm_verified() throws Exception {
            when(storeService.sellerHasStore(verifiedSeller.getId())).thenReturn(true);

            mockMvc.perform(get("/seller/products/new").with(user(verifiedSeller)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("seller/product-form"))
                    .andExpect(model().attributeExists("product"));
        }

        @Test
        @DisplayName("GET /seller/products/new redirects unverified seller")
        void newProductForm_unverified_redirects() throws Exception {
            mockMvc.perform(get("/seller/products/new").with(user(unverifiedSeller)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/seller/products"));
        }

        @Test
        @DisplayName("GET /seller/products/new redirects when seller has no store")
        void newProductForm_noStore_redirects() throws Exception {
            when(storeService.sellerHasStore(verifiedSeller.getId())).thenReturn(false);

            mockMvc.perform(get("/seller/products/new").with(user(verifiedSeller)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/seller/store"));
        }

        @Test
        @DisplayName("POST /seller/products creates product and redirects")
        void createProduct_success() throws Exception {
            when(productService.createProduct(any(ProductDto.class), eq(verifiedSeller)))
                    .thenReturn(product);

            mockMvc.perform(post("/seller/products")
                            .with(user(verifiedSeller))
                            .with(csrf())
                            .param("name", "New Product")
                            .param("description", "Desc")
                            .param("price", "29.99")
                            .param("stockQuantity", "10"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/seller/products"))
                    .andExpect(flash().attribute("message", containsString("created")));
        }

        @Test
        @DisplayName("POST /seller/products returns form when validation fails (blank name)")
        void createProduct_validationFail_returnsForm() throws Exception {
            mockMvc.perform(post("/seller/products")
                            .with(user(verifiedSeller))
                            .with(csrf())
                            .param("name", "")  // @NotBlank violated
                            .param("price", "29.99")
                            .param("stockQuantity", "5"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("seller/product-form"));
        }

        @Test
        @DisplayName("GET /seller/products/{id}/edit shows edit form for owner")
        void editProductForm_owner() throws Exception {
            when(productService.findById(product.getId())).thenReturn(Optional.of(product));

            mockMvc.perform(get("/seller/products/{id}/edit", product.getId())
                            .with(user(verifiedSeller)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("seller/product-form"))
                    .andExpect(model().attributeExists("product"));
        }

        @Test
        @DisplayName("GET /seller/products/{id}/edit redirects non-owner")
        void editProductForm_nonOwner_redirects() throws Exception {
            User otherSeller = User.builder().id(50L).username("other")
                    .role(Role.SELLER).verified(true).build();
            // product's store belongs to verifiedSeller (id=3), otherSeller (id=50) is not the owner
            when(productService.findById(product.getId())).thenReturn(Optional.of(product));

            mockMvc.perform(get("/seller/products/{id}/edit", product.getId())
                            .with(user(otherSeller)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/seller/products"));
        }

        @Test
        @DisplayName("POST /seller/products/{id}/delete deletes and redirects")
        void deleteProduct_success() throws Exception {
            doNothing().when(productService).deleteProduct(product.getId(), verifiedSeller);

            mockMvc.perform(post("/seller/products/{id}/delete", product.getId())
                            .with(user(verifiedSeller))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/seller/products"))
                    .andExpect(flash().attribute("message", containsString("deleted")));
        }

        @Test
        @DisplayName("POST /seller/products/{id}/delete flashes error when service throws")
        void deleteProduct_serviceError() throws Exception {
            doThrow(new RuntimeException("You can only delete your own products"))
                    .when(productService).deleteProduct(anyLong(), any());

            mockMvc.perform(post("/seller/products/{id}/delete", product.getId())
                            .with(user(verifiedSeller))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }
    }
}