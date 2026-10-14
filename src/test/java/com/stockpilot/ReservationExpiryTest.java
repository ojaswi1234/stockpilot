package com.stockpilot;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.dto.OrderDto;
import com.stockpilot.dto.OrderItemRequest;
import com.stockpilot.model.*;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.OrderService;
import com.stockpilot.service.ReservationExpiryJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ReservationExpiryTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationExpiryJob reservationExpiryJob;

    private Product testProduct;
    private Warehouse testWarehouse;
    private User testCustomer;

    @BeforeEach
    void setup() {
        testWarehouse = warehouseRepository.findByCode("WH-EXP").orElseGet(() ->
                warehouseRepository.save(new Warehouse("WH-EXP", "Expiry Test WH", "Location")));
        testProduct = productRepository.findBySku("SKU-EXP-01").orElseGet(() ->
                productRepository.save(new Product("SKU-EXP-01", "Expiring Item", "Description", new BigDecimal("40.00"), 5)));
        testCustomer = userRepository.findByUsername("customer_bob").orElseGet(() ->
                userRepository.save(new User("customer_bob", "pass", "Bob", "bob@exp.com", Role.CUSTOMER, null)));

        Inventory inv = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElse(null);
        if (inv != null) {
            inv.setQuantityOnHand(10);
            inv.setQuantityReserved(0);
            inventoryRepository.save(inv);
        } else {
            inventoryRepository.save(new Inventory(testProduct, testWarehouse, 10, 0));
        }
    }

    @Test
    @DisplayName("Scheduled sweep: Automatically cancels expired reservations and restores stock")
    void testScheduledExpiryReleasesStock() {
        CreateOrderRequest req = new CreateOrderRequest(testWarehouse.getId(),
                Collections.singletonList(new OrderItemRequest(testProduct.getId(), 5)));
        OrderDto orderDto = orderService.createAndReserveOrder(req, testCustomer);

        // Manually simulate expiration by setting timeout in the past
        Order order = orderRepository.findById(orderDto.getId()).orElseThrow();
        order.setReservationExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        orderRepository.save(order);

        // Run the scheduled sweeper
        reservationExpiryJob.sweepExpiredReservations();

        // Verify order is marked EXPIRED
        Order expiredOrder = orderRepository.findById(orderDto.getId()).orElseThrow();
        assertEquals(OrderStatus.EXPIRED, expiredOrder.getStatus());

        // Verify inventory reservation was released
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElseThrow();
        assertEquals(10, inventory.getQuantityOnHand());
        assertEquals(0, inventory.getQuantityReserved(), "Reserved quantity should be 0 after expiration");
        assertEquals(10, inventory.getAvailableQuantity(), "Available quantity should be fully restored to 10");
    }
}
