package com.stockpilot.service;

import com.stockpilot.dto.InventorySummaryDto;
import com.stockpilot.dto.StockAdjustmentRequest;
import com.stockpilot.dto.StockMovementDto;
import com.stockpilot.dto.StockReceiptRequest;
import com.stockpilot.event.LowStockAlertEvent;
import com.stockpilot.event.StockChangedEvent;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.model.*;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.StockMovementRepository;
import com.stockpilot.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final CsvSanitizer csvSanitizer;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository,
                            WarehouseRepository warehouseRepository,
                            StockMovementRepository stockMovementRepository,
                            ApplicationEventPublisher eventPublisher,
                            AuditService auditService,
                            CsvSanitizer csvSanitizer) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.csvSanitizer = csvSanitizer;
    }

    @Transactional(readOnly = true)
    public List<InventorySummaryDto> getInventorySummary() {
        return inventoryRepository.findAll().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventorySummaryDto> getInventoryByWarehouse(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventorySummaryDto> getLowStockAlerts() {
        return inventoryRepository.findLowStockInventory().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getRecentMovements() {
        return stockMovementRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toMovementDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getAllMovements() {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toMovementDto)
                .collect(Collectors.toList());
    }

    /**
     * Receives new shipment/purchase into warehouse inventory with pessimistic row locking.
     */
    @Transactional
    public InventorySummaryDto receiveStock(StockReceiptRequest req, User performedByUser) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + req.getProductId()));
        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + req.getWarehouseId()));

        Inventory inventory = inventoryRepository.findForUpdate(req.getProductId(), req.getWarehouseId())
                .orElseGet(() -> {
                    Inventory newInv = new Inventory(product, warehouse, 0, 0);
                    return inventoryRepository.save(newInv);
                });

        int prevOnHand = inventory.getQuantityOnHand();
        int prevReserved = inventory.getQuantityReserved();

        inventory.receive(req.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement(
                product, warehouse, MovementType.RECEIPT,
                req.getQuantity(), prevOnHand, saved.getQuantityOnHand(),
                prevReserved, saved.getQuantityReserved(),
                "RECEIPT-" + Instant.now().toEpochMilli(), "PURCHASE_RECEIPT",
                performedByUser, req.getNotes() != null ? req.getNotes() : "Shipment receipt"
        );
        stockMovementRepository.save(movement);

        auditService.record(performedByUser, "RECEIVE_STOCK", "INVENTORY",
                saved.getId().toString(), "Received " + req.getQuantity() + " units for SKU " + product.getSku());

        log.info("Stock received: +{} units of SKU {} into WH {}", req.getQuantity(), product.getSku(), warehouse.getCode());
        eventPublisher.publishEvent(new StockChangedEvent(saved, "Receipt +" + req.getQuantity()));

        return toSummaryDto(saved);
    }

    /**
     * Performs a manual stock reconciliation/adjustment with pessimistic row locking.
     */
    @Transactional
    public InventorySummaryDto adjustStock(StockAdjustmentRequest req, User performedByUser) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + req.getProductId()));
        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + req.getWarehouseId()));

        Inventory inventory = inventoryRepository.findForUpdate(req.getProductId(), req.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product and warehouse"));

        int prevOnHand = inventory.getQuantityOnHand();
        int prevReserved = inventory.getQuantityReserved();
        int diff = req.getNewQuantityOnHand() - prevOnHand;

        inventory.adjust(req.getNewQuantityOnHand());
        Inventory saved = inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement(
                product, warehouse, MovementType.ADJUSTMENT,
                diff, prevOnHand, saved.getQuantityOnHand(),
                prevReserved, saved.getQuantityReserved(),
                "ADJUST-" + Instant.now().toEpochMilli(), "MANUAL_ADJUSTMENT",
                performedByUser, req.getReason()
        );
        stockMovementRepository.save(movement);

        auditService.record(performedByUser, "ADJUST_STOCK", "INVENTORY",
                saved.getId().toString(), "Adjusted on-hand from " + prevOnHand + " to " + req.getNewQuantityOnHand() + " (" + req.getReason() + ")");

        log.info("Stock adjusted for SKU {} in WH {}: {} -> {} (reason: {})",
                product.getSku(), warehouse.getCode(), prevOnHand, req.getNewQuantityOnHand(), req.getReason());

        eventPublisher.publishEvent(new StockChangedEvent(saved, "Manual adjustment: " + req.getReason()));

        if (saved.getQuantityOnHand() <= product.getLowStockThreshold()) {
            eventPublisher.publishEvent(new LowStockAlertEvent(saved));
        }

        return toSummaryDto(saved);
    }

    /**
     * Exports inventory summary to CSV with formula injection sanitization.
     */
    public String exportInventoryToCsv(List<InventorySummaryDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("SKU,Product Name,Warehouse Code,Warehouse Name,On Hand,Reserved,Available,Unit Price,Low Stock Threshold,Low Stock\n");
        for (InventorySummaryDto item : items) {
            sb.append(csvSanitizer.sanitize(item.getSku())).append(",")
                    .append(csvSanitizer.sanitize(item.getProductName())).append(",")
                    .append(csvSanitizer.sanitize(item.getWarehouseCode())).append(",")
                    .append(csvSanitizer.sanitize(item.getWarehouseName())).append(",")
                    .append(item.getQuantityOnHand()).append(",")
                    .append(item.getQuantityReserved()).append(",")
                    .append(item.getAvailableQuantity()).append(",")
                    .append(item.getUnitPrice()).append(",")
                    .append(item.getLowStockThreshold()).append(",")
                    .append(item.isLowStock() ? "YES" : "NO").append("\n");
        }
        return sb.toString();
    }

    /**
     * Exports stock movement history to CSV with formula injection sanitization.
     */
    public String exportMovementsToCsv(List<StockMovementDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,SKU,Product Name,Warehouse,Movement Type,Qty Change,Prev OnHand,New OnHand,Ref ID,User,Notes\n");
        for (StockMovementDto m : items) {
            sb.append(m.getCreatedAt()).append(",")
                    .append(csvSanitizer.sanitize(m.getSku())).append(",")
                    .append(csvSanitizer.sanitize(m.getProductName())).append(",")
                    .append(csvSanitizer.sanitize(m.getWarehouseCode())).append(",")
                    .append(m.getMovementType()).append(",")
                    .append(m.getQuantityChange()).append(",")
                    .append(m.getPreviousOnHand()).append(",")
                    .append(m.getNewOnHand()).append(",")
                    .append(csvSanitizer.sanitize(m.getReferenceId())).append(",")
                    .append(csvSanitizer.sanitize(m.getPerformedByUsername())).append(",")
                    .append(csvSanitizer.sanitize(m.getNotes())).append("\n");
        }
        return sb.toString();
    }

    public InventorySummaryDto toSummaryDto(Inventory i) {
        boolean isLow = i.getQuantityOnHand() <= i.getProduct().getLowStockThreshold();
        return new InventorySummaryDto(
                i.getId(),
                i.getProduct().getId(),
                i.getProduct().getSku(),
                i.getProduct().getName(),
                i.getWarehouse().getId(),
                i.getWarehouse().getCode(),
                i.getWarehouse().getName(),
                i.getQuantityOnHand(),
                i.getQuantityReserved(),
                i.getAvailableQuantity(),
                i.getProduct().getUnitPrice(),
                i.getProduct().getLowStockThreshold(),
                isLow,
                i.getUpdatedAt()
        );
    }

    public StockMovementDto toMovementDto(StockMovement m) {
        String username = m.getPerformedByUser() != null ? m.getPerformedByUser().getUsername() : "SYSTEM";
        return new StockMovementDto(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getSku(),
                m.getProduct().getName(),
                m.getWarehouse().getId(),
                m.getWarehouse().getCode(),
                m.getMovementType(),
                m.getQuantityChange(),
                m.getPreviousOnHand(),
                m.getNewOnHand(),
                m.getPreviousReserved(),
                m.getNewReserved(),
                m.getReferenceId(),
                m.getReferenceType(),
                username,
                m.getNotes(),
                m.getCreatedAt()
        );
    }
}
