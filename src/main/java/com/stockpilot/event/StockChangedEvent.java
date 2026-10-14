package com.stockpilot.event;

import com.stockpilot.model.Inventory;

public class StockChangedEvent {

    private final Inventory inventory;
    private final String reason;

    public StockChangedEvent(Inventory inventory, String reason) {
        this.inventory = inventory;
        this.reason = reason;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public String getReason() {
        return reason;
    }
}
