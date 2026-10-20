package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.StockEntryItem;
import com.tiendalaesquina.domain.model.StockEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockEntryItemRepository extends JpaRepository<StockEntryItem, Long> {

    List<StockEntryItem> findByStockEntryIdOrderByIdAsc(Long stockEntryId);

    @Query("""
            SELECT item
            FROM StockEntryItem item
            JOIN FETCH item.stockEntry entry
            JOIN FETCH entry.supplier supplier
            JOIN FETCH item.product product
            WHERE product.id = :productId
              AND entry.status = :status
              AND supplier.active = true
            ORDER BY entry.entryDate DESC, item.id DESC
            """)
    List<StockEntryItem> findPriceHistoryForProduct(
            @Param("productId") Long productId,
            @Param("status") StockEntryStatus status
    );
}