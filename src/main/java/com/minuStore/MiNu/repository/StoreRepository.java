package com.minuStore.MiNu.repository;

import com.minuStore.MiNu.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findBySellerId(Long sellerId);

    boolean existsBySellerId(Long sellerId);
}
