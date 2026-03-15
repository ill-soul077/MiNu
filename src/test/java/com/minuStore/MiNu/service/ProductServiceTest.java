package com.minuStore.MiNu.service;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.dto.ProductDto;
import com.minuStore.MiNu.model.*;
import com.minuStore.MiNu.repository.ProductRepository;
import com.minuStore.MiNu.repository.StoreRepository;
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
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ProductService productService;

    private User verifiedSeller;
    private User unverifiedSeller;
    private User otherSeller;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        verifiedSeller = TestFixtures.verifiedSeller();
        unverifiedSeller = TestFixtures.unverifiedSeller();
        otherSeller = User.builder().id(50L).username("other").role(Role.SELLER)
                .verified(true).build();
        store = TestFixtures.store(verifiedSeller);
        product = TestFixtures.product(store);
    }

    // ══════════════════════════════════════════════════════════
    // findAll / findById / search
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Query Methods")
    class QueryTests {

        @Test
        @DisplayName("findAll returns all products")
        void findAll_returnsAll() {
            when(productRepository.findAll()).thenReturn(List.of(product));
            assertThat(productService.findAll()).containsExactly(product);
        }

        @Test
        @DisplayName("findAll returns empty list when no products")
        void findAll_empty() {
            when(productRepository.findAll()).thenReturn(Collections.emptyList());
            assertThat(productService.findAll()).isEmpty();
        }

        @Test
        @DisplayName("findById returns product when present")
        void findById_found() {
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            assertThat(productService.findById(product.getId())).contains(product);
        }

        @Test
        @DisplayName("findById returns empty when not present")
        void findById_notFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());
            assertThat(productService.findById(999L)).isEmpty();
        }

        @Test
        @DisplayName("findBySeller delegates to repository")
        void findBySeller() {
            when(productRepository.findByStoreSellerId(verifiedSeller.getId()))
                    .thenReturn(List.of(product));
            assertThat(productService.findBySeller(verifiedSeller.getId())).containsExactly(product);
        }

        @Test
        @DisplayName("searchByName is case-insensitive")
        void searchByName() {
            when(productRepository.findByNameContainingIgnoreCase("test"))
                    .thenReturn(List.of(product));
            assertThat(productService.searchByName("test")).containsExactly(product);
            verify(productRepository).findByNameContainingIgnoreCase("test");
        }
    }

    // ══════════════════════════════════════════════════════════
    // createProduct
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createProduct()")
    class CreateProductTests {

        @Test
        @DisplayName("should create product successfully for verified seller with store")
        void createProduct_success() {
            ProductDto dto = TestFixtures.productDto();
            when(storeRepository.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.of(store));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Product result = productService.createProduct(dto, verifiedSeller);

            assertThat(result.getName()).isEqualTo(dto.getName());
            assertThat(result.getPrice()).isEqualByComparingTo(dto.getPrice());
            assertThat(result.getStockQuantity()).isEqualTo(dto.getStockQuantity());
            assertThat(result.getStore()).isEqualTo(store);
        }

        @Test
        @DisplayName("should persist correct fields from DTO")
        void createProduct_persistsAllFields() {
            ProductDto dto = TestFixtures.productDto();
            when(storeRepository.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.of(store));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            when(productRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            productService.createProduct(dto, verifiedSeller);

            Product saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo(dto.getName());
            assertThat(saved.getDescription()).isEqualTo(dto.getDescription());
            assertThat(saved.getImageUrl()).isEqualTo(dto.getImageUrl());
        }

        @Test
        @DisplayName("should throw when seller is not verified")
        void createProduct_unverifiedSeller_throws() {
            assertThatThrownBy(() -> productService.createProduct(TestFixtures.productDto(), unverifiedSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("verified");

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("should throw when seller has no store")
        void createProduct_noStore_throws() {
            when(storeRepository.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(TestFixtures.productDto(), verifiedSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("store");
        }
    }

    // ══════════════════════════════════════════════════════════
    // updateProduct
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateProduct()")
    class UpdateProductTests {

        @Test
        @DisplayName("should update product successfully by the owner")
        void updateProduct_success() {
            ProductDto dto = ProductDto.builder()
                    .name("Updated Name").description("Updated").price(new BigDecimal("99.99"))
                    .stockQuantity(20).imageUrl("https://new.url").build();

            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Product result = productService.updateProduct(product.getId(), dto, verifiedSeller);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(result.getStockQuantity()).isEqualTo(20);
        }

        @Test
        @DisplayName("should throw when product not found")
        void updateProduct_notFound_throws() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(999L, TestFixtures.productDto(), verifiedSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("should throw when seller tries to edit another seller's product")
        void updateProduct_wrongSeller_throws() {
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            // otherSeller (id=50) is not the owner (verifiedSeller id=3)
            assertThatThrownBy(() -> productService.updateProduct(product.getId(), TestFixtures.productDto(), otherSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("own products");
        }
    }

    // ══════════════════════════════════════════════════════════
    // deleteProduct
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteProduct()")
    class DeleteProductTests {

        @Test
        @DisplayName("should delete product when owner requests it")
        void deleteProduct_ownerCanDelete() {
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            productService.deleteProduct(product.getId(), verifiedSeller);

            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("should throw when non-owner tries to delete")
        void deleteProduct_wrongSeller_throws() {
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.deleteProduct(product.getId(), otherSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("own products");

            verify(productRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw when product not found")
        void deleteProduct_notFound_throws() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(999L, verifiedSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("admin can delete any product")
        void deleteProductAsAdmin_success() {
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            productService.deleteProductAsAdmin(product.getId());

            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("admin delete throws when product not found")
        void deleteProductAsAdmin_notFound_throws() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProductAsAdmin(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product not found");
        }
    }
}