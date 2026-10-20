package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.StockNotification;
import com.tiendalaesquina.domain.model.StockNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface StockNotificationRepository
        extends JpaRepository<StockNotification, Long>,
        JpaSpecificationExecutor<StockNotification> {

    boolean existsByProductIdAndNotificationTypeAndIsReadFalse(
            Long productId,
            StockNotificationType notificationType
    );

    @Query("""
        select count(n)
        from StockNotification n
        where n.isRead = true
          and n.readAt is not null
          and n.readAt >= :from
          and n.readAt < :toExclusive
        """)
    long countResolvedAlerts(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );
}