package com.minuStore.MiNu.service;

import com.minuStore.MiNu.dto.StoreDto;
import com.minuStore.MiNu.model.Store;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public Optional<Store> findById(Long id) {
        return storeRepository.findById(id);
    }

    public Optional<Store> findBySellerId(Long sellerId) {
        return storeRepository.findBySellerId(sellerId);
    }

    public boolean sellerHasStore(Long sellerId) {
        return storeRepository.existsBySellerId(sellerId);
    }

    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    @Transactional
    public Store createStore(StoreDto dto, User seller) {
        if (!seller.isVerified()) {
            throw new RuntimeException("Your account must be verified before creating a store");
        }
        if (storeRepository.existsBySellerId(seller.getId())) {
            throw new RuntimeException("You already have a store");
        }

        Store store = Store.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .seller(seller)
                .build();

        return storeRepository.save(store);
    }

    @Transactional
    public Store updateStore(StoreDto dto, User seller) {
        Store store = storeRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new RuntimeException("Store not found"));

        store.setName(dto.getName());
        store.setDescription(dto.getDescription());

        return storeRepository.save(store);
    }
}