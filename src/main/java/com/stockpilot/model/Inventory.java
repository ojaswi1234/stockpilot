package com.stockpilot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.Instant;

@Entity
@Table(name = "inventory", uniqueConstraints = {
    @UniqueConstraint(name = "uq_product_warehouse", columnNames = {"product_id", "warehouse_id"})
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Min(0)
    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand = 0;

    @Min(0)
    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved = 0;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Inventory() {
    }

    public Inventory(Product product, Warehouse warehouse, int quantityOnHand, int quantityReserved) {
        this.product = product;
        this.warehouse = warehouse;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.version = 0L;
        this.updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        return Math.max(0, quantityOnHand - quantityReserved);
    }

    public boolean canReserve(int requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Cannot reserve " + quantity + " units; available: " + getAvailableQuantity());
        }
        this.quantityReserved += quantity;
        this.updatedAt = Instant.now();
    }

    public void releaseReservation(int quantity) {
        this.quantityReserved = Math.max(0, this.quantityReserved - quantity);
        this.updatedAt = Instant.now();
    }

    public void fulfill(int quantity) {
        if (this.quantityOnHand < quantity) {
            throw new IllegalStateException("Cannot fulfill " + quantity + " units; on-hand: " + this.quantityOnHand);
        }
        this.quantityOnHand -= quantity;
        this.quantityReserved = Math.max(0, this.quantityReserved - quantity);
        this.updatedAt = Instant.now();
    }

    public void receive(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Received quantity must be positive");
        }
        this.quantityOnHand += quantity;
        this.updatedAt = Instant.now();
    }

    public void adjust(int newOnHand) {
        if (newOnHand < this.quantityReserved) {
            throw new IllegalStateException("Adjusted on-hand (" + newOnHand + ") cannot be less than currently reserved (" + this.quantityReserved + ")");
        }
        this.quantityOnHand = newOnHand;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
