package com.stockpilot.repository;

import com.stockpilot.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<Inventory> findByWarehouseId(Long warehouseId);

    List<Inventory> findByProductId(Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.quantityOnHand <= i.product.lowStockThreshold")
    List<Inventory> findLowStockInventory();

    /**
     * Executes SELECT ... FOR UPDATE on the inventory row for pessimistic concurrency control.
     * Guarantees zero overselling under race conditions when multiple orders concurrently request stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
    Optional<Inventory> findForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);
}
