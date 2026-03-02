package com.minuStore.MiNu.repository;

import com.minuStore.MiNu.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(com.minuStore.MiNu.model.OrderStatus status);
}
