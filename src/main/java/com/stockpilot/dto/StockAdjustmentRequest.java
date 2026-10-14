package com.stockpilot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @Min(value = 0, message = "New on-hand quantity cannot be negative")
    private int newQuantityOnHand;

    @NotBlank(message = "Reason for stock adjustment is required")
    private String reason;

    public StockAdjustmentRequest() {
    }

    public StockAdjustmentRequest(Long productId, Long warehouseId, int newQuantityOnHand, String reason) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.newQuantityOnHand = newQuantityOnHand;
        this.reason = reason;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getNewQuantityOnHand() {
        return newQuantityOnHand;
    }

    public void setNewQuantityOnHand(int newQuantityOnHand) {
        this.newQuantityOnHand = newQuantityOnHand;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
