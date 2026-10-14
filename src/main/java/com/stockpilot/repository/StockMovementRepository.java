package com.stockpilot.repository;

import com.stockpilot.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<StockMovement> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
    List<StockMovement> findTop50ByOrderByCreatedAtDesc();
    List<StockMovement> findAllByOrderByCreatedAtDesc();
}
