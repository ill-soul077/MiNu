package com.minuStore.MiNu.service;

import com.minuStore.MiNu.dto.ProductDto;
import com.minuStore.MiNu.model.Product;
import com.minuStore.MiNu.model.Store;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.ProductRepository;
import com.minuStore.MiNu.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findBySeller(Long sellerId) {
        return productRepository.findByStoreSellerId(sellerId);
    }

    public List<Product> findByStore(Long storeId) {
        return productRepository.findByStoreId(storeId);
    }

    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Product createProduct(ProductDto dto, User seller) {
        if (!seller.isVerified()) {
            throw new RuntimeException("Your account must be verified before adding products");
        }

        Store store = storeRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new RuntimeException("You must create a store first"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .imageUrl(dto.getImageUrl())
                .store(store)
                .build();
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductDto dto, User seller) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getStore().getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("You can only edit your own products");
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setImageUrl(dto.getImageUrl());

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id, User seller) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getStore().getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("You can only delete your own products");
        }

        productRepository.delete(product);
    }

    @Transactional
    public void deleteProductAsAdmin(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productRepository.delete(product);
    }
}
