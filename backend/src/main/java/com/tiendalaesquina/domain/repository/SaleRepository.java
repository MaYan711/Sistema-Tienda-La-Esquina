package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SaleRepository
        extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    Optional<Sale> findBySaleNumber(String saleNumber);

    boolean existsBySaleNumber(String saleNumber);
}