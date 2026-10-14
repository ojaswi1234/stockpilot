package com.stockpilot;

import com.stockpilot.dto.InventorySummaryDto;
import com.stockpilot.dto.StockAdjustmentRequest;
import com.stockpilot.dto.StockReceiptRequest;
import com.stockpilot.model.*;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.StockMovementRepository;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private UserRepository userRepository;

    private Product testProduct;
    private Warehouse testWarehouse;
    private User testManager;

    @BeforeEach
    void setup() {
        testWarehouse = warehouseRepository.findByCode("WH-INV").orElseGet(() ->
                warehouseRepository.save(new Warehouse("WH-INV", "Inventory Test WH", "Location")));
        testProduct = productRepository.findBySku("SKU-INV-01").orElseGet(() ->
                productRepository.save(new Product("SKU-INV-01", "Inventory Item", "Desc", new BigDecimal("100.00"), 10)));
        testManager = userRepository.findByUsername("wh_manager_east").orElseGet(() ->
                userRepository.save(new User("wh_manager_east", "pass", "Elena", "elena@inv.com", Role.WAREHOUSE_MANAGER, testWarehouse)));

        Inventory inv = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElse(null);
        if (inv != null) {
            inv.setQuantityOnHand(20);
            inv.setQuantityReserved(0);
            inventoryRepository.save(inv);
        } else {
            inventoryRepository.save(new Inventory(testProduct, testWarehouse, 20, 0));
        }
    }

    @Test
    @DisplayName("Receive Stock: Successfully increases on-hand inventory and writes audit ledger")
    void testReceiveStock() {
        StockReceiptRequest req = new StockReceiptRequest(testProduct.getId(), testWarehouse.getId(), 15, "Inbound shipment");
        InventorySummaryDto result = inventoryService.receiveStock(req, testManager);

        assertEquals(35, result.getQuantityOnHand());
        assertEquals(35, result.getAvailableQuantity());

        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(testProduct.getId());
        assertFalse(movements.isEmpty());
        assertEquals(MovementType.RECEIPT, movements.get(0).getMovementType());
        assertEquals(15, movements.get(0).getQuantityChange());
    }

    @Test
    @DisplayName("Adjust Stock: Correctly updates inventory to match cycle count discrepancy")
    void testAdjustStock() {
        StockAdjustmentRequest req = new StockAdjustmentRequest(testProduct.getId(), testWarehouse.getId(), 8, "Damaged goods written off");
        InventorySummaryDto result = inventoryService.adjustStock(req, testManager);

        assertEquals(8, result.getQuantityOnHand());
        assertTrue(result.isLowStock(), "8 is <= threshold 10, should flag as low stock");

        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(testProduct.getId());
        assertFalse(movements.isEmpty());
        assertEquals(MovementType.ADJUSTMENT, movements.get(0).getMovementType());
        assertEquals(-12, movements.get(0).getQuantityChange());
    }
}
