package com.stockpilot.service;

import com.stockpilot.model.Order;
import com.stockpilot.model.OrderStatus;
import com.stockpilot.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public ReservationExpiryJob(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /**
     * Periodically scans for unfulfilled reservations that exceeded their timeout window
     * and automatically cancels them to return stock to available inventory.
     */
    @Scheduled(fixedDelay = 30000)
    public void sweepExpiredReservations() {
        List<Order> expiredOrders = orderRepository.findByStatusAndReservationExpiresAtBefore(
                OrderStatus.RESERVED, Instant.now());

        if (!expiredOrders.isEmpty()) {
            log.info("Found {} expired order reservations. Executing automatic release...", expiredOrders.size());
            for (Order order : expiredOrders) {
                try {
                    orderService.expireOrder(order.getId());
                } catch (Exception ex) {
                    log.error("Failed to expire order {}: {}", order.getOrderNumber(), ex.getMessage());
                }
            }
        }
    }
}
