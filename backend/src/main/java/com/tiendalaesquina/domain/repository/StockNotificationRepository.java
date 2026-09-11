package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.StockNotification;
import com.tiendalaesquina.domain.model.StockNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockNotificationRepository extends JpaRepository<StockNotification, Long>, JpaSpecificationExecutor<StockNotification> {

    boolean existsByProductIdAndNotificationTypeAndIsReadFalse(Long productId, StockNotificationType notificationType);
}
