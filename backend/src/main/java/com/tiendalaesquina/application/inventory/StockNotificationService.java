package com.tiendalaesquina.application.inventory;

import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.StockNotification;
import com.tiendalaesquina.domain.model.StockNotificationType;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.StockNotificationRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.StockNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class StockNotificationService {

    private final StockNotificationRepository notifications;
    private final UserAccountRepository users;

    public StockNotificationService(StockNotificationRepository notifications,
                                    UserAccountRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<StockNotificationResponse> searchNotifications(Long productId, String type,
                                                               Boolean isRead, Pageable pageable) {
        StockNotificationType notificationType = normalizeNotificationType(type);

        Specification<StockNotification> specification = (root, query, cb) -> cb.conjunction();

        if (productId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("product").get("id"), productId)
            );
        }

        if (notificationType != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("notificationType"), notificationType)
            );
        }

        if (isRead != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("isRead"), isRead)
            );
        }

        return notifications.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional
    public StockNotificationResponse markAsRead(Long id, String actorEmail) {
        UserAccount actor = users.findByEmail(actorEmail)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No fue posible identificar al usuario autenticado"
                ));

        StockNotification notification = notifications.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "notification_not_found",
                        "Notificación no encontrada",
                        "La notificación solicitada no existe"
                ));

        if (!notification.isRead()) {
            notification.markAsRead(actor);
            notification = notifications.save(notification);
        }

        return toResponse(notification);
    }

    private StockNotificationType normalizeNotificationType(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return StockNotificationType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_notification_type",
                    "Tipo de notificación inválido",
                    "El tipo de notificación debe ser LOW_STOCK u OUT_OF_STOCK"
            );
        }
    }

    private StockNotificationResponse toResponse(StockNotification notification) {
        Product product = notification.getProduct();
        UserAccount readBy = notification.getReadBy();

        return new StockNotificationResponse(
                notification.getId(),
                product.getId(),
                product.getCode(),
                product.getName(),
                notification.getNotificationType().name(),
                notification.getTitle(),
                notification.getMessage(),
                product.getCurrentStock(),
                product.getMinimumStock(),
                notification.isRead(),
                readBy != null ? readBy.getEmail() : null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
