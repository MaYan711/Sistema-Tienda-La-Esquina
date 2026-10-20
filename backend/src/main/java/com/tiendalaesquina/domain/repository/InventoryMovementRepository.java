package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long>, JpaSpecificationExecutor<InventoryMovement> {

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
}
