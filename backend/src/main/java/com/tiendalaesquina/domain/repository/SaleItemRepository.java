package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.SaleItem;
import com.tiendalaesquina.domain.model.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    @Query("""
        select coalesce(
            sum(
                si.lineTotal -
                (si.unitCostSnapshot * si.quantity)
            ),
            0
        )
        from SaleItem si
        where si.sale.status = :status
          and si.sale.saleDate >= :from
          and si.sale.saleDate < :toExclusive
        """)
    BigDecimal sumEstimatedMargin(
            @Param("status") SaleStatus status,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
    select coalesce(sum(si.quantity), 0)
    from SaleItem si
    where si.sale.status = :status
      and si.sale.saleDate >= :from
      and si.sale.saleDate < :toExclusive
    """)
    BigDecimal sumProductsSold(
            @Param("status") SaleStatus status,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
    select
        si.product.id as productId,
        si.product.code as productCode,
        si.product.name as productName,
        sum(si.quantity) as quantitySold,
        sum(si.lineTotal) as totalSales
    from SaleItem si
    where si.sale.status = :status
      and si.sale.saleDate >= :from
      and si.sale.saleDate < :toExclusive
    group by
        si.product.id,
        si.product.code,
        si.product.name
    order by
        sum(si.quantity) desc,
        sum(si.lineTotal) desc
    """)
    List<TopProductProjection> findTopProducts(
            @Param("status") SaleStatus status,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            Pageable pageable
    );

    @Query("""
    select
        si.product.category.id as categoryId,
        si.product.category.name as categoryName,
        sum(si.quantity) as productsSold,
        sum(si.lineTotal) as totalSales,
        sum(
            si.lineTotal -
            (si.unitCostSnapshot * si.quantity)
        ) as estimatedMargin
    from SaleItem si
    where si.sale.status = :status
      and si.sale.saleDate >= :from
      and si.sale.saleDate < :toExclusive
    group by
        si.product.category.id,
        si.product.category.name
    order by
        sum(si.lineTotal) desc
    """)
    List<CategoryReportProjection> findCategorySummary(
            @Param("status") SaleStatus status,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );
}