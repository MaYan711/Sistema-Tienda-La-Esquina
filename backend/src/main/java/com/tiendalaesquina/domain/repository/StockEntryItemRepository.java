package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.StockEntryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockEntryItemRepository extends JpaRepository<StockEntryItem, Long> {

    List<StockEntryItem> findByStockEntryIdOrderByIdAsc(Long stockEntryId);
}