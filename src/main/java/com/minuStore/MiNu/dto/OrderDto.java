package com.minuStore.MiNu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private Long customerId;

    @Builder.Default
    private List<OrderItemDto> items = new ArrayList<>();

    private String status;
}
