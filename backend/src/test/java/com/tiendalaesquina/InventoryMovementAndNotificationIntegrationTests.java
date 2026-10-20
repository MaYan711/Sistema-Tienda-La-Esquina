package com.tiendalaesquina;

import com.tiendalaesquina.domain.model.InventoryMovement;
import com.tiendalaesquina.domain.model.InventoryMovementType;
import com.tiendalaesquina.domain.model.InventorySourceType;
import com.tiendalaesquina.domain.model.MeasurementUnit;
import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.ProductCategory;
import com.tiendalaesquina.domain.model.Role;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.StockNotification;
import com.tiendalaesquina.domain.model.StockNotificationType;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.InventoryMovementRepository;
import com.tiendalaesquina.domain.repository.MeasurementUnitRepository;
import com.tiendalaesquina.domain.repository.ProductCategoryRepository;
import com.tiendalaesquina.domain.repository.ProductRepository;
import com.tiendalaesquina.domain.repository.RoleRepository;
import com.tiendalaesquina.domain.repository.StockNotificationRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CoreFlowIntegrationTests.TestMailConfiguration.class)
@Transactional
class InventoryMovementAndNotificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository products;

    @Autowired
    private ProductCategoryRepository categories;

    @Autowired
    private MeasurementUnitRepository units;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private InventoryMovementRepository movements;

    @Autowired
    private StockNotificationRepository notifications;

    @Autowired
    private JwtService jwtService;

    private UserAccount adminUser;
    private UserAccount employeeUser;
    private String adminToken;
    private String employeeToken;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByName(RoleName.ADMIN).orElseThrow();
        Role employeeRole = roles.findByName(RoleName.EMPLOYEE).orElseThrow();

        adminUser = users.save(new UserAccount(
                "admin-" + UUID.randomUUID() + "@example.com",
                "$2a$10$eACCtx.Y06.1w0i1uB/K2.N0LwW1YJpU60k3z4f4wK8H",
                adminRole,
                true
        ));

        employeeUser = users.save(new UserAccount(
                "employee-" + UUID.randomUUID() + "@example.com",
                "$2a$10$eACCtx.Y06.1w0i1uB/K2.N0LwW1YJpU60k3z4f4wK8H",
                employeeRole,
                true
        ));

        adminToken = jwtService.issue(adminUser);
        employeeToken = jwtService.issue(employeeUser);

        ProductCategory category = categories.findAll().stream().findFirst().orElseGet(() ->
                categories.save(new ProductCategory("Cat-" + UUID.randomUUID(), "Desc", adminUser))
        );

        MeasurementUnit unit = units.findByCodeIgnoreCase("UND").orElseGet(() ->
                units.save(new MeasurementUnit("UND", "Unidad", false))
        );

        product1 = products.save(new Product(
                "MOV1-" + UUID.randomUUID().toString().substring(0, 6),
                "Producto Mov 1",
                "Desc",
                null,
                category,
                unit,
                new BigDecimal("10.0000"),
                new BigDecimal("15.00"),
                new BigDecimal("50.000"),
                new BigDecimal("10.000"),
                adminUser
        ));

        product2 = products.save(new Product(
                "MOV2-" + UUID.randomUUID().toString().substring(0, 6),
                "Producto Mov 2",
                "Desc",
                null,
                category,
                unit,
                new BigDecimal("20.0000"),
                new BigDecimal("28.00"),
                new BigDecimal("5.000"),
                new BigDecimal("10.000"),
                adminUser
        ));
    }

    // ==========================================
    // 1. GET /inventory/movements Tests
    // ==========================================

    @Test
    void adminCanQueryInventoryMovementsWithDetails() throws Exception {
        InventoryMovement movement = movements.save(new InventoryMovement(
                product1,
                InventoryMovementType.ADJUSTMENT,
                new BigDecimal("5.000"),
                new BigDecimal("45.000"),
                new BigDecimal("50.000"),
                InventorySourceType.ADJUSTMENT,
                123L,
                "Ajuste de inventario físico",
                adminUser,
                Instant.now()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.id == %d)].productId".formatted(movement.getId())).value(product1.getId().intValue()))
                .andExpect(jsonPath("$.content[?(@.id == %d)].productCode".formatted(movement.getId())).value(product1.getCode()))
                .andExpect(jsonPath("$.content[?(@.id == %d)].productName".formatted(movement.getId())).value(product1.getName()))
                .andExpect(jsonPath("$.content[?(@.id == %d)].movementType".formatted(movement.getId())).value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].quantityDelta".formatted(movement.getId())).value(5.0))
                .andExpect(jsonPath("$.content[?(@.id == %d)].quantityBefore".formatted(movement.getId())).value(45.0))
                .andExpect(jsonPath("$.content[?(@.id == %d)].quantityAfter".formatted(movement.getId())).value(50.0))
                .andExpect(jsonPath("$.content[?(@.id == %d)].sourceType".formatted(movement.getId())).value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].sourceId".formatted(movement.getId())).value(123))
                .andExpect(jsonPath("$.content[?(@.id == %d)].reason".formatted(movement.getId())).value("Ajuste de inventario físico"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].createdByEmail".formatted(movement.getId())).value(adminUser.getEmail()))
                .andExpect(jsonPath("$.content[?(@.id == %d)].createdAt".formatted(movement.getId())).isNotEmpty());
    }

    @Test
    void employeeCannotQueryInventoryMovements() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/movements")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void unauthenticatedCannotQueryInventoryMovements() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/movements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void adminCanFilterMovementsByProductIdAndMovementType() throws Exception {
        InventoryMovement mov1 = movements.save(new InventoryMovement(
                product1,
                InventoryMovementType.ADJUSTMENT,
                new BigDecimal("5.000"),
                new BigDecimal("45.000"),
                new BigDecimal("50.000"),
                InventorySourceType.ADJUSTMENT,
                1L,
                "Ajuste prod 1",
                adminUser,
                Instant.now()
        ));

        InventoryMovement mov2 = movements.save(new InventoryMovement(
                product2,
                InventoryMovementType.SALE,
                new BigDecimal("-2.000"),
                new BigDecimal("7.000"),
                new BigDecimal("5.000"),
                InventorySourceType.SALE,
                2L,
                "Venta prod 2",
                adminUser,
                Instant.now()
        ));

        // Filtro por productId = product1.getId()
        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("productId", String.valueOf(product1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(mov1.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(mov2.getId())).doesNotExist());

        // Filtro por movementType = SALE
        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("movementType", "SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(mov2.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(mov1.getId())).doesNotExist());
    }

    @Test
    void rejectInvalidMovementType() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("movementType", "INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_movement_type"));
    }

    @Test
    void safeFallbackWhenInvalidSortByProvidedInMovements() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/inventory/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("sortBy", "nonExistentField; DROP TABLE products;"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ==========================================
    // 2. GET /notifications Tests
    // ==========================================

    @Test
    void adminCanQueryStockNotificationsWithDetails() throws Exception {
        StockNotification notification = notifications.save(new StockNotification(
                product2,
                StockNotificationType.LOW_STOCK,
                "Existencia baja",
                "El producto tiene existencias bajas"
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.id == %d)].productId".formatted(notification.getId())).value(product2.getId().intValue()))
                .andExpect(jsonPath("$.content[?(@.id == %d)].productCode".formatted(notification.getId())).value(product2.getCode()))
                .andExpect(jsonPath("$.content[?(@.id == %d)].productName".formatted(notification.getId())).value(product2.getName()))
                .andExpect(jsonPath("$.content[?(@.id == %d)].type".formatted(notification.getId())).value("LOW_STOCK"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].title".formatted(notification.getId())).value("Existencia baja"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].message".formatted(notification.getId())).value("El producto tiene existencias bajas"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].currentStock".formatted(notification.getId())).value(5.0))
                .andExpect(jsonPath("$.content[?(@.id == %d)].minimumStock".formatted(notification.getId())).value(10.0))
                .andExpect(jsonPath("$.content[?(@.id == %d)].read".formatted(notification.getId())).value(false))
                .andExpect(jsonPath("$.content[?(@.id == %d)].readByEmail".formatted(notification.getId())).value((Object) null))
                .andExpect(jsonPath("$.content[?(@.id == %d)].readAt".formatted(notification.getId())).value((Object) null))
                .andExpect(jsonPath("$.content[?(@.id == %d)].createdAt".formatted(notification.getId())).isNotEmpty());
    }

    @Test
    void employeeCannotQueryStockNotifications() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void unauthenticatedCannotQueryStockNotifications() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void adminCanFilterNotificationsByProductTypeAndIsRead() throws Exception {
        StockNotification n1 = notifications.save(new StockNotification(
                product1,
                StockNotificationType.OUT_OF_STOCK,
                "Agotado prod 1",
                "Mensaje 1"
        ));

        StockNotification n2 = new StockNotification(
                product2,
                StockNotificationType.LOW_STOCK,
                "Bajo prod 2",
                "Mensaje 2"
        );
        n2.markAsRead(adminUser);
        n2 = notifications.save(n2);

        // Filtro por productId = product1.getId()
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("productId", String.valueOf(product1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n1.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n2.getId())).doesNotExist());

        // Filtro por type = LOW_STOCK
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("type", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n2.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n1.getId())).doesNotExist());

        // Filtro por isRead = true
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("isRead", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n2.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n1.getId())).doesNotExist());

        // Filtro por isRead = false
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n1.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(n2.getId())).doesNotExist());
    }

    @Test
    void rejectInvalidNotificationType() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("type", "INVALID_NOTIF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_notification_type"));
    }

    @Test
    void safeFallbackWhenInvalidSortByProvidedInNotifications() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("sortBy", "malicious_column_name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ==========================================
    // 3. PATCH /notifications/{id}/read Tests
    // ==========================================

    @Test
    void adminCanMarkNotificationAsRead() throws Exception {
        StockNotification unreadNotification = notifications.save(new StockNotification(
                product1,
                StockNotificationType.LOW_STOCK,
                "Existencia baja",
                "Mensaje de prueba"
        ));

        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{id}/read", unreadNotification.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(unreadNotification.getId()))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readByEmail").value(adminUser.getEmail()))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // Verificar persistencia en base de datos
        StockNotification updated = notifications.findById(unreadNotification.getId()).orElseThrow();
        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadBy()).isNotNull();
        assertThat(updated.getReadBy().getEmail()).isEqualTo(adminUser.getEmail());
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    void markingNotificationAsReadIsIdempotent() throws Exception {
        StockNotification notification = notifications.save(new StockNotification(
                product1,
                StockNotificationType.OUT_OF_STOCK,
                "Agotado",
                "Mensaje de agotado"
        ));

        // Primera llamada
        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{id}/read", notification.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        StockNotification firstRead = notifications.findById(notification.getId()).orElseThrow();
        Instant originalReadAt = firstRead.getReadAt();
        String originalReadByEmail = firstRead.getReadBy().getEmail();

        // Crear otro usuario admin
        Role adminRole = roles.findByName(RoleName.ADMIN).orElseThrow();
        UserAccount secondAdmin = users.save(new UserAccount(
                "admin2-" + UUID.randomUUID() + "@example.com",
                "$2a$10$eACCtx.Y06.1w0i1uB/K2.N0LwW1YJpU60k3z4f4wK8H",
                adminRole,
                true
        ));
        String secondAdminToken = jwtService.issue(secondAdmin);

        // Segunda llamada con otro admin
        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{id}/read", notification.getId())
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readByEmail").value(originalReadByEmail));

        // Verificar que no se alteró la primera lectura
        StockNotification secondCheck = notifications.findById(notification.getId()).orElseThrow();
        assertThat(secondCheck.getReadBy().getEmail()).isEqualTo(originalReadByEmail);
        assertThat(secondCheck.getReadAt()).isEqualTo(originalReadAt);
    }

    @Test
    void markingNonExistentNotificationReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{id}/read", 99999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("notification_not_found"));
    }

    @Test
    void employeeCannotMarkNotificationAsRead() throws Exception {
        StockNotification notification = notifications.save(new StockNotification(
                product1,
                StockNotificationType.LOW_STOCK,
                "Existencia baja",
                "Mensaje"
        ));

        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{id}/read", notification.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void unauthenticatedCannotMarkNotificationAsRead() throws Exception {
        StockNotification notification = notifications.save(new StockNotification(
                product1,
                StockNotificationType.LOW_STOCK,
                "Existencia baja",
                "Mensaje"
        ));

        mockMvc.perform(MockMvcRequestBuilders.patch("/notifications/{id}/read", notification.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }
}
