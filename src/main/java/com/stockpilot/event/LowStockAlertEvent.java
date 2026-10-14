package com.stockpilot.event;

import com.stockpilot.model.Inventory;

public class LowStockAlertEvent {

    private final Inventory inventory;

    public LowStockAlertEvent(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
