package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierRepository
        extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    boolean existsByNitIgnoreCase(String nit);

    boolean existsByNitIgnoreCaseAndIdNot(String nit, Long id);

    long countByActiveTrue();
}