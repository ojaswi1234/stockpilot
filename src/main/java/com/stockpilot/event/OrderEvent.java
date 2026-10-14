package com.stockpilot.event;

import com.stockpilot.model.Order;

public class OrderEvent {

    private final Order order;
    private final String eventType; // ORDER_RESERVED, ORDER_PAID, ORDER_FULFILLED, ORDER_CANCELLED, ORDER_EXPIRED

    public OrderEvent(Order order, String eventType) {
        this.order = order;
        this.eventType = eventType;
    }

    public Order getOrder() {
        return order;
    }

    public String getEventType() {
        return eventType;
    }
}
