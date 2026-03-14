package com.minuStore.MiNu.service;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.dto.StoreDto;
import com.minuStore.MiNu.model.Store;
import com.minuStore.MiNu.model.User;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService Unit Tests")
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    private User verifiedSeller;
    private User unverifiedSeller;
    private Store store;

    @BeforeEach
    void setUp() {
        verifiedSeller = TestFixtures.verifiedSeller();
        unverifiedSeller = TestFixtures.unverifiedSeller();
        store = TestFixtures.store(verifiedSeller);
    }

    // ══════════════════════════════════════════════════════════
    // findById / findBySellerId / sellerHasStore / findAll
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Query Methods")
    class QueryTests {

        @Test
        @DisplayName("findById returns store when present")
        void findById_found() {
            when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
            assertThat(storeService.findById(store.getId())).contains(store);
        }

        @Test
        @DisplayName("findById returns empty when missing")
        void findById_notFound() {
            when(storeRepository.findById(999L)).thenReturn(Optional.empty());
            assertThat(storeService.findById(999L)).isEmpty();
        }

        @Test
        @DisplayName("findBySellerId returns store for that seller")
        void findBySellerId_found() {
            when(storeRepository.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.of(store));
            assertThat(storeService.findBySellerId(verifiedSeller.getId())).contains(store);
        }

        @Test
        @DisplayName("sellerHasStore returns true when store exists")
        void sellerHasStore_true() {
            when(storeRepository.existsBySellerId(verifiedSeller.getId())).thenReturn(true);
            assertThat(storeService.sellerHasStore(verifiedSeller.getId())).isTrue();
        }

        @Test
        @DisplayName("sellerHasStore returns false when no store")
        void sellerHasStore_false() {
            when(storeRepository.existsBySellerId(verifiedSeller.getId())).thenReturn(false);
            assertThat(storeService.sellerHasStore(verifiedSeller.getId())).isFalse();
        }

        @Test
        @DisplayName("findAll returns all stores")
        void findAll_returnsAll() {
            when(storeRepository.findAll()).thenReturn(List.of(store));
            assertThat(storeService.findAll()).containsExactly(store);
        }
    }

    // ══════════════════════════════════════════════════════════
    // createStore
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createStore()")
    class CreateStoreTests {

        @Test
        @DisplayName("should create store for verified seller without existing store")
        void createStore_success() {
            StoreDto dto = TestFixtures.storeDto();
            when(storeRepository.existsBySellerId(verifiedSeller.getId())).thenReturn(false);
            when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Store result = storeService.createStore(dto, verifiedSeller);

            assertThat(result.getName()).isEqualTo(dto.getName());
            assertThat(result.getDescription()).isEqualTo(dto.getDescription());
            assertThat(result.getSeller()).isEqualTo(verifiedSeller);
        }

        @Test
        @DisplayName("should persist store with correct seller reference")
        void createStore_persistsCorrectSeller() {
            StoreDto dto = TestFixtures.storeDto();
            when(storeRepository.existsBySellerId(verifiedSeller.getId())).thenReturn(false);

            ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
            when(storeRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            storeService.createStore(dto, verifiedSeller);

            assertThat(captor.getValue().getSeller()).isEqualTo(verifiedSeller);
        }

        @Test
        @DisplayName("should throw when seller is not verified")
        void createStore_unverifiedSeller_throws() {
            assertThatThrownBy(() -> storeService.createStore(TestFixtures.storeDto(), unverifiedSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("verified");

            verifyNoInteractions(storeRepository);
        }

        @Test
        @DisplayName("should throw when seller already has a store")
        void createStore_alreadyHasStore_throws() {
            when(storeRepository.existsBySellerId(verifiedSeller.getId())).thenReturn(true);

            assertThatThrownBy(() -> storeService.createStore(TestFixtures.storeDto(), verifiedSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already have a store");

            verify(storeRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    // updateStore
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateStore()")
    class UpdateStoreTests {

        @Test
        @DisplayName("should update store name and description")
        void updateStore_success() {
            StoreDto dto = StoreDto.builder().name("New Name").description("New Desc").build();
            when(storeRepository.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.of(store));
            when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Store result = storeService.updateStore(dto, verifiedSeller);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("New Desc");
        }

        @Test
        @DisplayName("should throw when seller has no store to update")
        void updateStore_noStore_throws() {
            when(storeRepository.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.updateStore(TestFixtures.storeDto(), verifiedSeller))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Store not found");
        }

        @Test
        @DisplayName("should not create a new store, only update existing")
        void updateStore_doesNotCreateNew() {
            when(storeRepository.findBySellerId(verifiedSeller.getId())).thenReturn(Optional.of(store));
            when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            storeService.updateStore(TestFixtures.storeDto(), verifiedSeller);

            // save called once (update), never findAll or existsBy
            verify(storeRepository, times(1)).save(any());
            verify(storeRepository, never()).existsBySellerId(any());
        }
    }
}