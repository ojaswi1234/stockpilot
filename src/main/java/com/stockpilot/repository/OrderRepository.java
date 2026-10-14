package com.stockpilot.repository;

import com.stockpilot.model.Order;
import com.stockpilot.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusAndReservationExpiresAtBefore(OrderStatus status, Instant expiryThreshold);
}
