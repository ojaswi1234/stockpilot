package com.stockpilot.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public StockEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockChanged(StockChangedEvent event) {
        var inv = event.getInventory();
        log.info("Dispatching stock changed event for SKU: {} in WH: {} (OnHand: {}, Reserved: {}, Available: {})",
                inv.getProduct().getSku(), inv.getWarehouse().getCode(),
                inv.getQuantityOnHand(), inv.getQuantityReserved(), inv.getAvailableQuantity());

        StockEventMessage msg = new StockEventMessage("STOCK_UPDATED", "Stock Level Updated",
                "SKU " + inv.getProduct().getSku() + " updated in warehouse " + inv.getWarehouse().getCode() + " (" + event.getReason() + ")");
        msg.setProductId(inv.getProduct().getId());
        msg.setSku(inv.getProduct().getSku());
        msg.setWarehouseId(inv.getWarehouse().getId());
        msg.setWarehouseCode(inv.getWarehouse().getCode());
        msg.setQuantityOnHand(inv.getQuantityOnHand());
        msg.setAvailableQuantity(inv.getAvailableQuantity());

        messagingTemplate.convertAndSend("/topic/inventory", msg);
        messagingTemplate.convertAndSend("/topic/warehouses." + inv.getWarehouse().getId(), msg);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLowStockAlert(LowStockAlertEvent event) {
        var inv = event.getInventory();
        log.warn("LOW STOCK ALERT: SKU {} in WH {} dropped to {} on hand (Threshold: {})",
                inv.getProduct().getSku(), inv.getWarehouse().getCode(),
                inv.getQuantityOnHand(), inv.getProduct().getLowStockThreshold());

        StockEventMessage msg = new StockEventMessage("LOW_STOCK_ALERT", "Low Stock Alert!",
                "Critical stock alert: Product " + inv.getProduct().getName() + " (" + inv.getProduct().getSku() + ") in " + inv.getWarehouse().getName() + " has only " + inv.getQuantityOnHand() + " remaining!");
        msg.setProductId(inv.getProduct().getId());
        msg.setSku(inv.getProduct().getSku());
        msg.setWarehouseId(inv.getWarehouse().getId());
        msg.setWarehouseCode(inv.getWarehouse().getCode());
        msg.setQuantityOnHand(inv.getQuantityOnHand());
        msg.setAvailableQuantity(inv.getAvailableQuantity());

        messagingTemplate.convertAndSend("/topic/inventory", msg);
        messagingTemplate.convertAndSend("/topic/warehouses." + inv.getWarehouse().getId(), msg);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderEvent(OrderEvent event) {
        var order = event.getOrder();
        log.info("Dispatching order event: {} for order: {}", event.getEventType(), order.getOrderNumber());

        StockEventMessage msg = new StockEventMessage("ORDER_STATUS_CHANGED", "Order " + order.getOrderNumber() + " Updated",
                "Order status changed to " + order.getStatus() + " (" + event.getEventType() + ")");
        msg.setOrderNumber(order.getOrderNumber());
        msg.setOrderStatus(order.getStatus().name());
        msg.setWarehouseId(order.getWarehouse().getId());
        msg.setWarehouseCode(order.getWarehouse().getCode());

        messagingTemplate.convertAndSend("/topic/orders", msg);
        messagingTemplate.convertAndSend("/topic/orders." + order.getOrderNumber(), msg);
    }
}
