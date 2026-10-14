package com.stockpilot.dto;

import com.stockpilot.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderDto {

    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerUsername;
    private String customerName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Instant reservationExpiresAt;
    private List<OrderItemDto> items = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public OrderDto() {
    }

    public OrderDto(Long id, String orderNumber, Long customerId, String customerUsername,
                    String customerName, Long warehouseId, String warehouseCode,
                    String warehouseName, OrderStatus status, BigDecimal totalAmount,
                    Instant reservationExpiresAt, List<OrderItemDto> items,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.customerUsername = customerUsername;
        this.customerName = customerName;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.reservationExpiresAt = reservationExpiresAt;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public void setReservationExpiresAt(Instant reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
