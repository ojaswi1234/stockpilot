package com.stockpilot.controller;

import com.stockpilot.dto.InventorySummaryDto;
import com.stockpilot.dto.OrderDto;
import com.stockpilot.dto.StockMovementDto;
import com.stockpilot.service.InventoryService;
import com.stockpilot.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class InventoryApiController {

    private final InventoryService inventoryService;
    private final OrderService orderService;

    public InventoryApiController(InventoryService inventoryService, OrderService orderService) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
    }

    @GetMapping("/inventory")
    public List<InventorySummaryDto> getInventory() {
        return inventoryService.getInventorySummary();
    }

    @GetMapping("/inventory/low-stock")
    public List<InventorySummaryDto> getLowStockAlerts() {
        return inventoryService.getLowStockAlerts();
    }

    @GetMapping("/movements")
    public List<StockMovementDto> getMovements() {
        return inventoryService.getRecentMovements();
    }

    @GetMapping("/orders")
    public List<OrderDto> getOrders() {
        return orderService.getAllOrders();
    }
}
