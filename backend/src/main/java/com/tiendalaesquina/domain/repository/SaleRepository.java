package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.Sale;
import com.tiendalaesquina.domain.model.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface SaleRepository
        extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    Optional<Sale> findBySaleNumber(String saleNumber);

    boolean existsBySaleNumber(String saleNumber);

    List<Sale> findByStatusAndSaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateAsc(
            SaleStatus status,
            Instant from,
            Instant toExclusive
    );

    @Query("""
        select coalesce(sum(s.totalAmount), 0)
        from Sale s
        where s.status = :status
          and s.saleDate >= :from
          and s.saleDate < :toExclusive
        """)
    BigDecimal sumTotalSales(
            @Param("status") SaleStatus status,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
        select count(s)
        from Sale s
        where s.status = :status
          and s.saleDate >= :from
          and s.saleDate < :toExclusive
        """)
    long countSales(
            @Param("status") SaleStatus status,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );


}