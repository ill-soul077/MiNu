package com.minuStore.MiNu.service;

import com.minuStore.MiNu.dto.OrderItemDto;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@SessionScope
@Getter
public class CartService {

    private final List<OrderItemDto> items = new ArrayList<>();

    public void addItem(Long productId, Integer quantity) {
        Optional<OrderItemDto> existing = items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            items.add(OrderItemDto.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build());
        }
    }

    public void removeItem(Long productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
    }

    public void clear() {
        items.clear();
    }

    public int getItemCount() {
        return items.stream().mapToInt(OrderItemDto::getQuantity).sum();
    }
}
