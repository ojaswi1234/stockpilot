package com.stockpilot.dto;

import com.stockpilot.model.MovementType;
import java.time.Instant;

public class StockMovementDto {

    private Long id;
    private Long productId;
    private String sku;
    private String productName;
    private Long warehouseId;
    private String warehouseCode;
    private MovementType movementType;
    private int quantityChange;
    private int previousOnHand;
    private int newOnHand;
    private int previousReserved;
    private int newReserved;
    private String referenceId;
    private String referenceType;
    private String performedByUsername;
    private String notes;
    private Instant createdAt;

    public StockMovementDto() {
    }

    public StockMovementDto(Long id, Long productId, String sku, String productName,
                            Long warehouseId, String warehouseCode, MovementType movementType,
                            int quantityChange, int previousOnHand, int newOnHand,
                            int previousReserved, int newReserved, String referenceId,
                            String referenceType, String performedByUsername, String notes,
                            Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.previousOnHand = previousOnHand;
        this.newOnHand = newOnHand;
        this.previousReserved = previousReserved;
        this.newReserved = newReserved;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.performedByUsername = performedByUsername;
        this.notes = notes;
        this.createdAt = createdAt;
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

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
    }

    public int getPreviousOnHand() {
        return previousOnHand;
    }

    public void setPreviousOnHand(int previousOnHand) {
        this.previousOnHand = previousOnHand;
    }

    public int getNewOnHand() {
        return newOnHand;
    }

    public void setNewOnHand(int newOnHand) {
        this.newOnHand = newOnHand;
    }

    public int getPreviousReserved() {
        return previousReserved;
    }

    public void setPreviousReserved(int previousReserved) {
        this.previousReserved = previousReserved;
    }

    public int getNewReserved() {
        return newReserved;
    }

    public void setNewReserved(int newReserved) {
        this.newReserved = newReserved;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getPerformedByUsername() {
        return performedByUsername;
    }

    public void setPerformedByUsername(String performedByUsername) {
        this.performedByUsername = performedByUsername;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
