package com.minuStore.MiNu.service;

import com.minuStore.MiNu.dto.OrderItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CartService Unit Tests")
class CartServiceTest {

    // CartService has NO external dependencies — no mocking needed
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService();
    }

    @Test
    @DisplayName("cart starts empty")
    void cart_startsEmpty() {
        assertThat(cartService.getItems()).isEmpty();
        assertThat(cartService.getItemCount()).isZero();
    }

    @Test
    @DisplayName("addItem adds a new product to cart")
    void addItem_addsNewProduct() {
        cartService.addItem(1L, 2);

        assertThat(cartService.getItems()).hasSize(1);
        assertThat(cartService.getItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(cartService.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("addItem accumulates quantity when same product added twice")
    void addItem_sameProduct_accumulatesQuantity() {
        cartService.addItem(1L, 2);
        cartService.addItem(1L, 3);

        assertThat(cartService.getItems()).hasSize(1);
        assertThat(cartService.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("addItem keeps separate entries for different products")
    void addItem_differentProducts_separateEntries() {
        cartService.addItem(1L, 1);
        cartService.addItem(2L, 2);

        assertThat(cartService.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("getItemCount returns total quantity across all items")
    void getItemCount_totalQuantity() {
        cartService.addItem(1L, 3);
        cartService.addItem(2L, 2);

        assertThat(cartService.getItemCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("removeItem removes only the specified product")
    void removeItem_removesCorrectProduct() {
        cartService.addItem(1L, 1);
        cartService.addItem(2L, 2);

        cartService.removeItem(1L);

        assertThat(cartService.getItems()).hasSize(1);
        assertThat(cartService.getItems().get(0).getProductId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("removeItem on non-existent product does nothing")
    void removeItem_nonExistent_noEffect() {
        cartService.addItem(1L, 1);
        cartService.removeItem(999L);

        assertThat(cartService.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("clear removes all items")
    void clear_removesAll() {
        cartService.addItem(1L, 1);
        cartService.addItem(2L, 2);
        cartService.addItem(3L, 3);

        cartService.clear();

        assertThat(cartService.getItems()).isEmpty();
        assertThat(cartService.getItemCount()).isZero();
    }

    @Test
    @DisplayName("getItemCount returns 0 after clear")
    void getItemCount_afterClear_isZero() {
        cartService.addItem(1L, 10);
        cartService.clear();

        assertThat(cartService.getItemCount()).isZero();
    }

    @Test
    @DisplayName("can re-add items after clear")
    void canAddAfterClear() {
        cartService.addItem(1L, 5);
        cartService.clear();
        cartService.addItem(2L, 3);

        assertThat(cartService.getItems()).hasSize(1);
        assertThat(cartService.getItemCount()).isEqualTo(3);
    }
}