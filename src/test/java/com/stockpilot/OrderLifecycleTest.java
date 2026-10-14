package com.stockpilot;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.dto.OrderDto;
import com.stockpilot.dto.OrderItemRequest;
import com.stockpilot.exception.IllegalOrderStateException;
import com.stockpilot.model.*;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class OrderLifecycleTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    private Product testProduct;
    private Warehouse testWarehouse;
    private User testCustomer;
    private User testManager;

    @BeforeEach
    void setup() {
        testWarehouse = warehouseRepository.findByCode("WH-LC").orElseGet(() ->
                warehouseRepository.save(new Warehouse("WH-LC", "Lifecycle Test WH", "Location")));
        testProduct = productRepository.findBySku("SKU-LC-01").orElseGet(() ->
                productRepository.save(new Product("SKU-LC-01", "Lifecycle Item", "Description", new BigDecimal("50.00"), 5)));
        testCustomer = userRepository.findByUsername("customer_bob").orElseGet(() ->
                userRepository.save(new User("customer_bob", "pass", "Bob", "bob@lc.com", Role.CUSTOMER, null)));
        testManager = userRepository.findByUsername("wh_manager_east").orElseGet(() ->
                userRepository.save(new User("wh_manager_east", "pass", "Elena", "elena@lc.com", Role.WAREHOUSE_MANAGER, testWarehouse)));

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
    @DisplayName("Order Lifecycle: RESERVED -> PAID -> FULFILLED properly updates stock invariants")
    void testFullOrderFulfillmentLifecycle() {
        // 1. Create and Reserve
        CreateOrderRequest req = new CreateOrderRequest(testWarehouse.getId(),
                Collections.singletonList(new OrderItemRequest(testProduct.getId(), 3)));
        OrderDto order = orderService.createAndReserveOrder(req, testCustomer);

        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals(new BigDecimal("150.00"), order.getTotalAmount());

        Inventory afterReserve = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElseThrow();
        assertEquals(10, afterReserve.getQuantityOnHand());
        assertEquals(3, afterReserve.getQuantityReserved());
        assertEquals(7, afterReserve.getAvailableQuantity());

        // 2. Pay
        OrderDto paidOrder = orderService.payOrder(order.getId(), testCustomer);
        assertEquals(OrderStatus.PAID, paidOrder.getStatus());

        // 3. Fulfill
        OrderDto fulfilledOrder = orderService.fulfillOrder(paidOrder.getId(), testManager);
        assertEquals(OrderStatus.FULFILLED, fulfilledOrder.getStatus());

        Inventory afterFulfill = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElseThrow();
        assertEquals(7, afterFulfill.getQuantityOnHand(), "On-hand should be deducted to 7");
        assertEquals(0, afterFulfill.getQuantityReserved(), "Reserved should be returned to 0");
        assertEquals(7, afterFulfill.getAvailableQuantity(), "Available should be 7");
    }

    @Test
    @DisplayName("Order Cancellation: Cancelling a reserved order returns reserved stock to available")
    void testOrderCancellationReleasesReservedStock() {
        CreateOrderRequest req = new CreateOrderRequest(testWarehouse.getId(),
                Collections.singletonList(new OrderItemRequest(testProduct.getId(), 4)));
        OrderDto order = orderService.createAndReserveOrder(req, testCustomer);

        Inventory afterReserve = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElseThrow();
        assertEquals(4, afterReserve.getQuantityReserved());
        assertEquals(6, afterReserve.getAvailableQuantity());

        // Cancel order
        OrderDto cancelled = orderService.cancelOrder(order.getId(), testCustomer, "Customer changed mind");
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());

        Inventory afterCancel = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElseThrow();
        assertEquals(10, afterCancel.getQuantityOnHand());
        assertEquals(0, afterCancel.getQuantityReserved(), "Reserved quantity must be restored to 0");
        assertEquals(10, afterCancel.getAvailableQuantity(), "Available quantity must be restored to 10");
    }

    @Test
    @DisplayName("Illegal transition: Cannot fulfill an order that is already cancelled")
    void testIllegalTransitionThrowsException() {
        CreateOrderRequest req = new CreateOrderRequest(testWarehouse.getId(),
                Collections.singletonList(new OrderItemRequest(testProduct.getId(), 2)));
        OrderDto order = orderService.createAndReserveOrder(req, testCustomer);
        orderService.cancelOrder(order.getId(), testCustomer, "Cancelled");

        assertThrows(IllegalOrderStateException.class, () ->
                orderService.fulfillOrder(order.getId(), testManager));
    }
}
