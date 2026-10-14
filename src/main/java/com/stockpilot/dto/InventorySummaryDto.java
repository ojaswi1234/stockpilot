package com.stockpilot.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class InventorySummaryDto {

    private Long id;
    private Long productId;
    private String sku;
    private String productName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private int quantityOnHand;
    private int quantityReserved;
    private int availableQuantity;
    private BigDecimal unitPrice;
    private int lowStockThreshold;
    private boolean lowStock;
    private Instant updatedAt;

    public InventorySummaryDto() {
    }

    public InventorySummaryDto(Long id, Long productId, String sku, String productName,
                               Long warehouseId, String warehouseCode, String warehouseName,
                               int quantityOnHand, int quantityReserved, int availableQuantity,
                               BigDecimal unitPrice, int lowStockThreshold, boolean lowStock,
                               Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.availableQuantity = availableQuantity;
        this.unitPrice = unitPrice;
        this.lowStockThreshold = lowStockThreshold;
        this.lowStock = lowStock;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public void setLowStock(boolean lowStock) {
        this.lowStock = lowStock;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
