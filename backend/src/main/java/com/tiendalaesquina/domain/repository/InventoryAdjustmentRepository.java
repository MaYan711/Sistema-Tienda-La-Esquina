package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    List<InventoryAdjustment> findByProductIdOrderByAdjustmentDateDesc(Long productId);
}
