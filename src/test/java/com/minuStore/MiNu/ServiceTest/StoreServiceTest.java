package com.minuStore.MiNu.ServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.minuStore.MiNu.dto.StoreDto;
import com.minuStore.MiNu.model.Store;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.StoreRepository;
import com.minuStore.MiNu.service.StoreService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

public class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    private User seller;
    private StoreDto dto;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        seller = new User();
        seller.setId(1L);
        seller.setVerified(true);

        dto = new StoreDto();
        dto.setName("My Store");
        dto.setDescription("Best products");
    }

    @Test
    void createStore_success() {

        when(storeRepository.existsBySellerId(1L)).thenReturn(false);

        Store savedStore = Store.builder()
                .name("My Store")
                .description("Best products")
                .seller(seller)
                .build();

        when(storeRepository.save(any(Store.class))).thenReturn(savedStore);

        Store result = storeService.createStore(dto, seller);

        assertNotNull(result);
        assertEquals("My Store", result.getName());

        verify(storeRepository).save(any(Store.class));
    }

    @Test
    void createStore_notVerifiedSeller() {

        seller.setVerified(false);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> storeService.createStore(dto, seller)
        );

        assertEquals("Your account must be verified before creating a store", ex.getMessage());
    }

    @Test
    void createStore_storeAlreadyExists() {

        when(storeRepository.existsBySellerId(1L)).thenReturn(true);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> storeService.createStore(dto, seller)
        );

        assertEquals("You already have a store", ex.getMessage());
    }

    @Test
    void updateStore_success() {

        Store existingStore = Store.builder()
                .name("Old Store")
                .description("Old description")
                .seller(seller)
                .build();

        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(existingStore));
        when(storeRepository.save(any(Store.class))).thenReturn(existingStore);

        Store result = storeService.updateStore(dto, seller);

        assertEquals("My Store", result.getName());
        assertEquals("Best products", result.getDescription());
    }

    @Test
    void updateStore_storeNotFound() {

        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> storeService.updateStore(dto, seller)
        );

        assertEquals("Store not found", ex.getMessage());
    }
}