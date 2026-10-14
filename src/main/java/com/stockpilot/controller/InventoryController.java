package com.stockpilot.controller;

import com.stockpilot.dto.InventorySummaryDto;
import com.stockpilot.dto.StockAdjustmentRequest;
import com.stockpilot.dto.StockMovementDto;
import com.stockpilot.dto.StockReceiptRequest;
import com.stockpilot.model.User;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    public InventoryController(InventoryService inventoryService, UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.userRepository = userRepository;
    }

    @PostMapping("/inventory/receive")
    public String receiveStock(@Valid @ModelAttribute StockReceiptRequest req,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        inventoryService.receiveStock(req, user);
        redirectAttributes.addFlashAttribute("successMessage", "Stock successfully received!");
        return "redirect:/";
    }

    @PostMapping("/inventory/adjust")
    public String adjustStock(@Valid @ModelAttribute StockAdjustmentRequest req,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        inventoryService.adjustStock(req, user);
        redirectAttributes.addFlashAttribute("successMessage", "Inventory successfully adjusted!");
        return "redirect:/";
    }

    @GetMapping("/inventory/export")
    public ResponseEntity<byte[]> exportInventory() {
        List<InventorySummaryDto> list = inventoryService.getInventorySummary();
        String csv = inventoryService.exportInventoryToCsv(list);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/inventory/movements/export")
    public ResponseEntity<byte[]> exportMovements() {
        List<StockMovementDto> list = inventoryService.getAllMovements();
        String csv = inventoryService.exportMovementsToCsv(list);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock-movements.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
