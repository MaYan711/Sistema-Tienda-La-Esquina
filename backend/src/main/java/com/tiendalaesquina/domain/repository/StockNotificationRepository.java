package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.StockNotification;
import com.tiendalaesquina.domain.model.StockNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockNotificationRepository extends JpaRepository<StockNotification, Long> {

    boolean existsByProductIdAndNotificationTypeAndIsReadFalse(Long productId, StockNotificationType notificationType);
}
