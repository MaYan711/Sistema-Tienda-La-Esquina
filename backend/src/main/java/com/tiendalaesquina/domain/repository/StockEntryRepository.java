package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockEntryRepository
        extends JpaRepository<StockEntry, Long>, JpaSpecificationExecutor<StockEntry> {
}