package com.stockpilot;

import com.stockpilot.event.LowStockAlertEvent;
import com.stockpilot.event.OrderEvent;
import com.stockpilot.event.StockChangedEvent;
import com.stockpilot.event.StockEventListener;
import com.stockpilot.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StockEventListenerTest {

    private SimpMessagingTemplate messagingTemplate;
    private StockEventListener listener;

    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;

    @BeforeEach
    void setup() {
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        listener = new StockEventListener(messagingTemplate);

        product = new Product("SKU-TEST-01", "Test Product", "Desc", new BigDecimal("10.00"), 5);
        product.setId(101L);

        warehouse = new Warehouse("WH-TEST", "Test WH", "Location");
        warehouse.setId(201L);

        inventory = new Inventory(product, warehouse, 10, 2);
        inventory.setId(301L);
    }

    @Test
    @DisplayName("StockEventListener: Dispatches stock update message to /topic/inventory")
    void testStockChangedEventBroadcast() {
        StockChangedEvent event = new StockChangedEvent(inventory, "Test Adjustment");
        listener.handleStockChanged(event);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/inventory"), any(Object.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/warehouses.201"), any(Object.class));
    }

    @Test
    @DisplayName("StockEventListener: Dispatches low stock alert to /topic/inventory")
    void testLowStockAlertBroadcast() {
        LowStockAlertEvent event = new LowStockAlertEvent(inventory);
        listener.handleLowStockAlert(event);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/inventory"), any(Object.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/warehouses.201"), any(Object.class));
    }

    @Test
    @DisplayName("StockEventListener: Dispatches order event to /topic/orders")
    void testOrderEventBroadcast() {
        User user = new User("cust", "p", "Customer", "c@test.com", Role.CUSTOMER, null);
        Order order = new Order("ORD-9999", user, warehouse);
        order.setStatus(OrderStatus.RESERVED);

        OrderEvent event = new OrderEvent(order, "ORDER_RESERVED");
        listener.handleOrderEvent(event);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/orders"), any(Object.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/orders.ORD-9999"), any(Object.class));
    }
}
