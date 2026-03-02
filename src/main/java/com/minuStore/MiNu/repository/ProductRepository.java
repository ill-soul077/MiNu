package com.minuStore.MiNu.repository;

import com.minuStore.MiNu.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);

    List<Product> findByStoreSellerId(Long sellerId);

    List<Product> findByNameContainingIgnoreCase(String name);
}
