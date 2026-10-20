package com.tiendalaesquina;

import com.tiendalaesquina.application.inventory.InventoryService;
import com.tiendalaesquina.domain.model.InventoryMovementType;
import com.tiendalaesquina.domain.model.InventorySourceType;
import com.tiendalaesquina.domain.model.MeasurementUnit;
import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.ProductCategory;
import com.tiendalaesquina.domain.model.Role;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.StockNotificationType;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.AuditEventRepository;
import com.tiendalaesquina.domain.repository.InventoryAdjustmentRepository;
import com.tiendalaesquina.domain.repository.InventoryMovementRepository;
import com.tiendalaesquina.domain.repository.MeasurementUnitRepository;
import com.tiendalaesquina.domain.repository.ProductCategoryRepository;
import com.tiendalaesquina.domain.repository.ProductRepository;
import com.tiendalaesquina.domain.repository.RoleRepository;
import com.tiendalaesquina.domain.repository.StockNotificationRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.security.JwtService;
import com.tiendalaesquina.web.dto.InventoryAdjustmentRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CoreFlowIntegrationTests.TestMailConfiguration.class)
@Transactional
class InventoryAdjustmentIntegrationTests {

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
    private InventoryAdjustmentRepository adjustments;

    @Autowired
    private InventoryMovementRepository movements;

    @Autowired
    private StockNotificationRepository notifications;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private InventoryService inventoryService;

    private UserAccount adminUser;
    private UserAccount employeeUser;
    private String adminToken;
    private String employeeToken;
    private Product testProduct;
    private MeasurementUnit unitUnd;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByName(RoleName.ADMIN).orElseThrow();
        Role employeeRole = roles.findByName(RoleName.EMPLOYEE).orElseThrow();

        adminUser = users.findByEmail("admin-test-" + UUID.randomUUID() + "@example.com")
                .orElseGet(() -> users.save(new UserAccount(
                        "admin-test-" + UUID.randomUUID() + "@example.com",
                        "$2a$10$eACCtx.Y06.1w0i1uB/K2.N0LwW1YJpU60k3z4f4wK8H",
                        adminRole,
                        true
                )));

        employeeUser = users.save(new UserAccount(
                "employee-test-" + UUID.randomUUID() + "@example.com",
                "$2a$10$eACCtx.Y06.1w0i1uB/K2.N0LwW1YJpU60k3z4f4wK8H",
                employeeRole,
                true
        ));

        adminToken = jwtService.issue(adminUser);
        employeeToken = jwtService.issue(employeeUser);

        ProductCategory category = categories.findAll().stream().findFirst().orElseGet(() ->
                categories.save(new ProductCategory("Test Cat " + UUID.randomUUID(), "Desc", adminUser))
        );

        unitUnd = units.findByCodeIgnoreCase("UND").orElseGet(() ->
                units.save(new MeasurementUnit("UND", "Unidad", false))
        );

        testProduct = products.save(new Product(
                "TEST-" + UUID.randomUUID().toString().substring(0, 8),
                "Producto Test Ajuste",
                "Descripcion producto",
                null,
                category,
                unitUnd,
                new BigDecimal("5.0000"),
                new BigDecimal("10.00"),
                new BigDecimal("20.000"),
                new BigDecimal("5.000"),
                adminUser
        ));
    }

    @Test
    void adminCanAdjustStockSuccessfullyWithMovementAndAudit() throws Exception {
        String payload = """
                {
                    "productId": %d,
                    "newStock": 25.000,
                    "reason": "Conteo físico mensual"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.productCode").value(testProduct.getCode()))
                .andExpect(jsonPath("$.quantityBefore").value(20.0))
                .andExpect(jsonPath("$.quantityAfter").value(25.0))
                .andExpect(jsonPath("$.quantityDelta").value(5.0))
                .andExpect(jsonPath("$.reason").value("Conteo físico mensual"))
                .andExpect(jsonPath("$.adjustedByEmail").value(adminUser.getEmail()))
                .andExpect(jsonPath("$.stockStatus").value("AVAILABLE"));

        // Verificar que el producto fue actualizado en BD
        Product updated = products.findById(testProduct.getId()).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualByComparingTo(new BigDecimal("25.000"));

        // Verificar registro de ajuste
        var productAdjustments = adjustments.findByProductIdOrderByAdjustmentDateDesc(testProduct.getId());
        assertThat(productAdjustments).isNotEmpty();
        var adj = productAdjustments.getFirst();
        assertThat(adj.getQuantityBefore()).isEqualByComparingTo(new BigDecimal("20.000"));
        assertThat(adj.getQuantityAfter()).isEqualByComparingTo(new BigDecimal("25.000"));
        assertThat(adj.getQuantityDelta()).isEqualByComparingTo(new BigDecimal("5.000"));
        assertThat(adj.getReason()).isEqualTo("Conteo físico mensual");
        assertThat(adj.getAdjustedBy().getId()).isEqualTo(adminUser.getId());

        // Verificar registro de movimiento
        var productMovements = movements.findByProductIdOrderByCreatedAtDesc(testProduct.getId());
        assertThat(productMovements).isNotEmpty();
        var mov = productMovements.getFirst();
        assertThat(mov.getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT);
        assertThat(mov.getSourceType()).isEqualTo(InventorySourceType.ADJUSTMENT);
        assertThat(mov.getSourceId()).isEqualTo(adj.getId());
        assertThat(mov.getQuantityDelta()).isEqualByComparingTo(new BigDecimal("5.000"));
        assertThat(mov.getQuantityBefore()).isEqualByComparingTo(new BigDecimal("20.000"));
        assertThat(mov.getQuantityAfter()).isEqualByComparingTo(new BigDecimal("25.000"));
        assertThat(mov.getCreatedBy().getId()).isEqualTo(adminUser.getId());

        // Verificar registro de auditoría
        var events = auditEvents.findAll().stream()
                .filter(e -> "INVENTORY_ADJUSTMENT".equals(e.getAction())
                        && String.valueOf(testProduct.getId()).equals(e.getEntityId()))
                .toList();
        assertThat(events).isNotEmpty();
        var audit = events.getLast();
        assertThat(audit.getActorUser().getId()).isEqualTo(adminUser.getId());
        assertThat(audit.getEntityType()).isEqualTo("PRODUCT");
        assertThat(audit.getBeforeData()).isNotNull();
        assertThat(audit.getAfterData()).isNotNull();
    }

    @Test
    void employeeCannotAdjustStock() throws Exception {
        String payload = """
                {
                    "productId": %d,
                    "newStock": 15.000,
                    "reason": "Intento no autorizado"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void anonymousCannotAdjustStock() throws Exception {
        String payload = """
                {
                    "productId": %d,
                    "newStock": 15.000,
                    "reason": "Intento sin token"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void rejectNegativeStock() throws Exception {
        String payload = """
                {
                    "productId": %d,
                    "newStock": -1.000,
                    "reason": "Stock negativo"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectStockWithExcessiveDecimals() throws Exception {
        String payload = """
                {
                    "productId": %d,
                    "newStock": 10.1234,
                    "reason": "Más de 3 decimales"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectAdjustmentWithoutStockChange() throws Exception {
        String payload = """
                {
                    "productId": %d,
                    "newStock": 20.000,
                    "reason": "Mismo stock"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("no_stock_change"));
    }

    @Test
    void rejectBlankReason() throws Exception {
        String payload = """
                {
                    "productId": %d,
                    "newStock": 15.000,
                    "reason": "   "
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectNonExistentProduct() throws Exception {
        String payload = """
                {
                    "productId": 99999999,
                    "newStock": 10.000,
                    "reason": "Producto fantasma"
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product_not_found"));
    }

    @Test
    void rejectInactiveProduct() throws Exception {
        testProduct.setActive(false, adminUser);
        products.save(testProduct);

        String payload = """
                {
                    "productId": %d,
                    "newStock": 10.000,
                    "reason": "Ajuste en inactivo"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("product_inactive"));
    }

    @Test
    void rejectDecimalsWhenUnitDoesNotAllow() throws Exception {
        // unitUnd allowsDecimal = false
        String payload = """
                {
                    "productId": %d,
                    "newStock": 10.500,
                    "reason": "Decimales en unidad entera"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("decimals_not_allowed"));
    }

    @Test
    void createsAlertWhenStockBecomesLowOrOutOfStockWithoutDuplicates() throws Exception {
        // 1. Ajustar a 0 -> OUT_OF_STOCK
        String payloadOutOfStock = """
                {
                    "productId": %d,
                    "newStock": 0.000,
                    "reason": "Agotado por merma"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadOutOfStock))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockStatus").value("OUT_OF_STOCK"));

        boolean hasOutOfStockAlert = notifications.existsByProductIdAndNotificationTypeAndIsReadFalse(
                testProduct.getId(), StockNotificationType.OUT_OF_STOCK
        );
        assertThat(hasOutOfStockAlert).isTrue();

        // 2. Ajustar a 3 (minimumStock es 5) -> LOW_STOCK
        String payloadLowStock = """
                {
                    "productId": %d,
                    "newStock": 3.000,
                    "reason": "Encontradas 3 unidades"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadLowStock))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockStatus").value("LOW_STOCK"));

        boolean hasLowStockAlert = notifications.existsByProductIdAndNotificationTypeAndIsReadFalse(
                testProduct.getId(), StockNotificationType.LOW_STOCK
        );
        assertThat(hasLowStockAlert).isTrue();

        // 3. Otro ajuste en LOW_STOCK no debe duplicar alerta pendiente
        long countBefore = notifications.findAll().stream()
                .filter(n -> n.getProduct().getId().equals(testProduct.getId())
                        && n.getNotificationType() == StockNotificationType.LOW_STOCK
                        && !n.isRead())
                .count();
        assertThat(countBefore).isEqualTo(1);

        String payloadAnotherLowStock = """
                {
                    "productId": %d,
                    "newStock": 4.000,
                    "reason": "Encontrada 1 unidad más"
                }
                """.formatted(testProduct.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadAnotherLowStock))
                .andExpect(status().isCreated());

        long countAfter = notifications.findAll().stream()
                .filter(n -> n.getProduct().getId().equals(testProduct.getId())
                        && n.getNotificationType() == StockNotificationType.LOW_STOCK
                        && !n.isRead())
                .count();
        assertThat(countAfter).isEqualTo(1);
    }

    @Test
    void transactionRollbackWhenOperationFailsLeavesStockAndRecordsUntouched() {
        BigDecimal initialStock = testProduct.getCurrentStock();
        long adjustmentsCountBefore = adjustments.count();
        long movementsCountBefore = movements.count();

        // Intentar un ajuste que falle en la lógica de negocio (por ejemplo, con unidad no decimal intentando decimales)
        InventoryAdjustmentRequest invalidRequest = new InventoryAdjustmentRequest(
                testProduct.getId(),
                new BigDecimal("12.500"),
                "Intento fallido"
        );

        assertThatThrownBy(() -> inventoryService.adjustStock(invalidRequest, adminUser.getEmail()))
                .isInstanceOf(ApiException.class);

        // Verificar que el stock no cambió y no se persistieron ajustes ni movimientos
        Product reloaded = products.findById(testProduct.getId()).orElseThrow();
        assertThat(reloaded.getCurrentStock()).isEqualByComparingTo(initialStock);
        assertThat(adjustments.count()).isEqualTo(adjustmentsCountBefore);
        assertThat(movements.count()).isEqualTo(movementsCountBefore);
    }
}
