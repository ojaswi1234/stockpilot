package com.stockpilot.controller;

import com.stockpilot.dto.InventorySummaryDto;
import com.stockpilot.dto.OrderDto;
import com.stockpilot.dto.StockMovementDto;
import com.stockpilot.model.User;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.InventoryService;
import com.stockpilot.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class DashboardController {

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    public DashboardController(InventoryService inventoryService,
                               OrderService orderService,
                               ProductRepository productRepository,
                               WarehouseRepository warehouseRepository,
                               UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        List<InventorySummaryDto> inventory = inventoryService.getInventorySummary();
        List<InventorySummaryDto> lowStockAlerts = inventoryService.getLowStockAlerts();
        List<StockMovementDto> recentMovements = inventoryService.getRecentMovements();
        List<OrderDto> allOrders = orderService.getAllOrders();

        BigDecimal totalValuation = inventory.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantityOnHand())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("productCount", productRepository.count());
        model.addAttribute("warehouseCount", warehouseRepository.count());
        model.addAttribute("lowStockCount", lowStockAlerts.size());
        model.addAttribute("totalValuation", totalValuation);
        model.addAttribute("activeOrdersCount", allOrders.stream().filter(o -> "RESERVED".equals(o.getStatus().name()) || "PAID".equals(o.getStatus().name())).count());
        model.addAttribute("inventory", inventory);
        model.addAttribute("lowStockAlerts", lowStockAlerts);
        model.addAttribute("recentMovements", recentMovements);
        model.addAttribute("warehouses", warehouseRepository.findByActiveTrue());
        model.addAttribute("products", productRepository.findByActiveTrue());

        return "dashboard";
    }
}
