package com.stockpilot.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "previous_on_hand", nullable = false)
    private int previousOnHand;

    @Column(name = "new_on_hand", nullable = false)
    private int newOnHand;

    @Column(name = "previous_reserved", nullable = false)
    private int previousReserved;

    @Column(name = "new_reserved", nullable = false)
    private int newReserved;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by_user_id")
    private User performedByUser;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StockMovement() {
    }

    public StockMovement(Product product, Warehouse warehouse, MovementType movementType,
                         int quantityChange, int previousOnHand, int newOnHand,
                         int previousReserved, int newReserved, String referenceId,
                         String referenceType, User performedByUser, String notes) {
        this.product = product;
        this.warehouse = warehouse;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.previousOnHand = previousOnHand;
        this.newOnHand = newOnHand;
        this.previousReserved = previousReserved;
        this.newReserved = newReserved;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.performedByUser = performedByUser;
        this.notes = notes;
        this.createdAt = Instant.now();
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

    public User getPerformedByUser() {
        return performedByUser;
    }

    public void setPerformedByUser(User performedByUser) {
        this.performedByUser = performedByUser;
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
