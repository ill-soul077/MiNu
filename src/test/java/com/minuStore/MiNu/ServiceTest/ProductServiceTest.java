package com.minuStore.MiNu.ServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.minuStore.MiNu.dto.ProductDto;
import com.minuStore.MiNu.model.Product;
import com.minuStore.MiNu.model.Store;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.ProductRepository;
import com.minuStore.MiNu.repository.StoreRepository;
import com.minuStore.MiNu.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ProductService productService;

    private User seller;
    private Store store;
    private ProductDto dto;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        seller = new User();
        seller.setId(1L);
        seller.setVerified(true);

        store = new Store();
        store.setId(10L);
        store.setSeller(seller);

        dto = new ProductDto();
        dto.setName("Laptop");
        dto.setDescription("Gaming laptop");
        dto.setPrice(BigDecimal.valueOf(1500.0));
        dto.setStockQuantity(5);
        dto.setImageUrl("img.png");
    }

    @Test
    void createProduct_success() {

        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));

        Product savedProduct = Product.builder()
                .name("Laptop")
                .description("Gaming laptop")
                .price(BigDecimal.valueOf(1500.0))
                .stockQuantity(5)
                .imageUrl("img.png")
                .store(store)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        Product result = productService.createProduct(dto, seller);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_notVerifiedSeller() {

        seller.setVerified(false);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> productService.createProduct(dto, seller)
        );

        assertEquals("Your account must be verified before adding products", ex.getMessage());
    }

    @Test
    void createProduct_storeNotFound() {

        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> productService.createProduct(dto, seller)
        );

        assertEquals("You must create a store first", ex.getMessage());
    }

    @Test
    void updateProduct_success() {

        Product product = new Product();
        product.setId(1L);
        product.setStore(store);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.updateProduct(1L, dto, seller);

        assertEquals("Laptop", result.getName());
        assertEquals("Gaming laptop", result.getDescription());
    }

    @Test
    void updateProduct_notOwner() {

        User otherSeller = new User();
        otherSeller.setId(2L);

        Product product = new Product();
        product.setStore(store);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> productService.updateProduct(1L, dto, otherSeller)
        );

        assertEquals("You can only edit your own products", ex.getMessage());
    }

    @Test
    void deleteProduct_success() {

        Product product = new Product();
        product.setStore(store);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L, seller);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_notOwner() {

        User otherSeller = new User();
        otherSeller.setId(2L);

        Product product = new Product();
        product.setStore(store);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> productService.deleteProduct(1L, otherSeller)
        );

        assertEquals("You can only delete your own products", ex.getMessage());
    }

    @Test
    void deleteProductAsAdmin_success() {

        Product product = new Product();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProductAsAdmin(1L);

        verify(productRepository).delete(product);
    }
}