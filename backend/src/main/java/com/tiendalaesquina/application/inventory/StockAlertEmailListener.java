package com.tiendalaesquina.application.inventory;

import com.tiendalaesquina.application.mail.EmailService;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class StockAlertEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockAlertEmailListener.class);

    private final UserAccountRepository users;
    private final EmailService emailService;

    public StockAlertEmailListener(UserAccountRepository users, EmailService emailService) {
        this.users = users;
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAlertToAdministrators(StockAlertEmailEvent event) {
        List<UserAccount> administrators = users.findActiveVerifiedByRole(RoleName.ADMIN);

        if (administrators.isEmpty()) {
            LOGGER.warn(
                    "No hay administradores activos y verificados para recibir la alerta del producto {}",
                    event.productId()
            );
            return;
        }

        for (UserAccount administrator : administrators) {
            try {
                emailService.sendStockAlert(
                        administrator.getEmail(),
                        event.notificationType(),
                        event.productCode(),
                        event.productName(),
                        event.currentStock(),
                        event.minimumStock()
                );
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "No fue posible enviar la alerta de inventario del producto {} al administrador {}",
                        event.productId(),
                        administrator.getEmail(),
                        exception
                );
            }
        }
    }
}
