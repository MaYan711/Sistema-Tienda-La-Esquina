package com.tiendalaesquina;

import com.tiendalaesquina.domain.model.Role;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.AuditEventRepository;
import com.tiendalaesquina.domain.repository.RoleRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CoreFlowIntegrationTests.TestMailConfiguration.class)
@Transactional
class UserAdminIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserAccount adminUser;
    private UserAccount employeeUser;
    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByName(RoleName.ADMIN).orElseThrow();
        Role employeeRole = roles.findByName(RoleName.EMPLOYEE).orElseThrow();

        adminUser = users.save(new UserAccount(
            "admin-" + UUID.randomUUID() + "@example.com",
            passwordEncoder.encode("Password123"),
            adminRole,
            true
        ));

        employeeUser = users.save(new UserAccount(
            "employee-" + UUID.randomUUID() + "@example.com",
            passwordEncoder.encode("Password123"),
            employeeRole,
            true
        ));

        adminToken = jwtService.issue(adminUser);
        employeeToken = jwtService.issue(employeeUser);
    }

    @Test
    void adminCanListUsersWithFiltersAndPagination() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "email")
                .param("direction", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());

        // Filtrar por búsqueda de email
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("search", adminUser.getEmail()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value(adminUser.getEmail()));

        // Filtrar por rol
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("role", "EMPLOYEE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.role == 'EMPLOYEE')]").isNotEmpty());

        // Filtrar por enabled
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("enabled", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.enabled == true)]").isNotEmpty());
    }

    @Test
    void adminCanGetUserById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/" + employeeUser.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(employeeUser.getId()))
            .andExpect(jsonPath("$.email").value(employeeUser.getEmail()))
            .andExpect(jsonPath("$.role").value("EMPLOYEE"))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getNonExistentUserReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/99999999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("user_not_found"));
    }

    @Test
    void adminCanCreateEmployeeSuccessfully() throws Exception {
        String newEmail = "new-emp-" + UUID.randomUUID() + "@example.com";
        String payload = """
            {
                "email": "%s",
                "password": "Password123",
                "role": "EMPLOYEE"
            }
            """.formatted(newEmail);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email").value(newEmail.toLowerCase()))
            .andExpect(jsonPath("$.role").value("EMPLOYEE"))
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn();

        Long createdId = Long.parseLong(extract(result.getResponse().getContentAsString(), "id"));

        UserAccount persisted = users.findById(createdId).orElseThrow();
        assertThat(persisted.getEmail()).isEqualTo(newEmail.toLowerCase());
        assertThat(persisted.isVerified()).isTrue();
        assertThat(persisted.isEnabled()).isTrue();
        assertThat(persisted.getRole().getName()).isEqualTo(RoleName.EMPLOYEE);
        assertThat(passwordEncoder.matches("Password123", persisted.getPasswordHash())).isTrue();

        // Verificar evento de auditoría
        boolean auditPresent = auditEvents.findAll().stream()
            .anyMatch(a -> "USER_CREATED".equals(a.getAction())
                && String.valueOf(createdId).equals(a.getEntityId())
                && a.getActorUser().getId().equals(adminUser.getId()));
        assertThat(auditPresent).isTrue();
    }

    @Test
    void rejectDuplicateEmailOnCreateAndEdit() throws Exception {
        // En creación
        String payloadDuplicate = """
            {
                "email": "%s",
                "password": "Password123",
                "role": "EMPLOYEE"
            }
            """.formatted(employeeUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadDuplicate))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("email_already_registered"));

        // En edición
        String payloadEditDuplicate = """
            {
                "email": "%s",
                "role": "EMPLOYEE"
            }
            """.formatted(adminUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + employeeUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadEditDuplicate))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("email_already_registered"));
    }

    @Test
    void rejectInvalidRoleOnCreateAndEdit() throws Exception {
        String payloadInvalidRole = """
            {
                "email": "invalid-role@example.com",
                "password": "Password123",
                "role": "SUPERADMIN"
            }
            """;

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadInvalidRole))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid_role"));

        String payloadEditInvalidRole = """
            {
                "email": "%s",
                "role": "MANAGER"
            }
            """.formatted(employeeUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + employeeUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadEditInvalidRole))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid_role"));
    }

    @Test
    void rejectInvalidOrWeakPassword() throws Exception {
        String payloadShortPassword = """
            {
                "email": "weak-pass@example.com",
                "password": "pass",
                "role": "EMPLOYEE"
            }
            """;

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadShortPassword))
            .andExpect(status().isBadRequest());

        String payloadNoDigits = """
            {
                "email": "weak-pass2@example.com",
                "password": "PasswordOnly",
                "role": "EMPLOYEE"
            }
            """;

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadNoDigits))
            .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanEditUserEmailAndRole() throws Exception {
        int initialTokenVersion = employeeUser.getTokenVersion();
        String updatedEmail = "updated-emp-" + UUID.randomUUID() + "@example.com";

        String payload = """
            {
                "email": "%s",
                "role": "ADMIN"
            }
            """.formatted(updatedEmail);

        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + employeeUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(employeeUser.getId()))
            .andExpect(jsonPath("$.email").value(updatedEmail.toLowerCase()))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        UserAccount reloaded = users.findById(employeeUser.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo(updatedEmail.toLowerCase());
        assertThat(reloaded.getRole().getName()).isEqualTo(RoleName.ADMIN);
        assertThat(reloaded.getTokenVersion()).isGreaterThan(initialTokenVersion);

        // Verificar auditoría
        boolean auditPresent = auditEvents.findAll().stream()
            .anyMatch(a -> a.getEntityId().equals(String.valueOf(employeeUser.getId()))
                && a.getActorUser().getId().equals(adminUser.getId()));
        assertThat(auditPresent).isTrue();
    }

    @Test
    void adminCanDeactivateAndActivateUser() throws Exception {
        int initialTokenVersion = employeeUser.getTokenVersion();

        // 1. Desactivar
        String payloadDeactivate = """
            {
                "enabled": false
            }
            """;

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/" + employeeUser.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadDeactivate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(employeeUser.getId()))
            .andExpect(jsonPath("$.enabled").value(false));

        UserAccount deactivated = users.findById(employeeUser.getId()).orElseThrow();
        assertThat(deactivated.isEnabled()).isFalse();
        assertThat(deactivated.getTokenVersion()).isEqualTo(initialTokenVersion + 1);

        // Verificar auditoría de desactivación
        boolean deactAudit = auditEvents.findAll().stream()
            .anyMatch(a -> "USER_DEACTIVATED".equals(a.getAction())
                && a.getEntityId().equals(String.valueOf(employeeUser.getId()))
                && a.getActorUser().getId().equals(adminUser.getId()));
        assertThat(deactAudit).isTrue();

        // 2. Reactivar
        String payloadActivate = """
            {
                "enabled": true
            }
            """;

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/" + employeeUser.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadActivate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(employeeUser.getId()))
            .andExpect(jsonPath("$.enabled").value(true));

        UserAccount activated = users.findById(employeeUser.getId()).orElseThrow();
        assertThat(activated.isEnabled()).isTrue();
        assertThat(activated.getTokenVersion()).isEqualTo(initialTokenVersion + 2);

        // Verificar auditoría de activación
        boolean actAudit = auditEvents.findAll().stream()
            .anyMatch(a -> "USER_ACTIVATED".equals(a.getAction())
                && a.getEntityId().equals(String.valueOf(employeeUser.getId()))
                && a.getActorUser().getId().equals(adminUser.getId()));
        assertThat(actAudit).isTrue();
    }

    @Test
    void deactivatedUserCannotAuthenticateAndPreviousJwtIsInvalidated() throws Exception {
        String password = "Password123";
        Role employeeRole = roles.findByName(RoleName.EMPLOYEE).orElseThrow();
        UserAccount user = users.save(new UserAccount(
            "to-disable-" + UUID.randomUUID() + "@example.com",
            passwordEncoder.encode(password),
            employeeRole,
            true
        ));
        String priorToken = jwtService.issue(user);

        // Token funcionaba previamente
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/me")
                .header("Authorization", "Bearer " + priorToken))
            .andExpect(status().isOk());

        // Desactivar el usuario
        mockMvc.perform(MockMvcRequestBuilders.patch("/users/" + user.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": false}"))
            .andExpect(status().isOk());

        // 1. El token previo ahora es rechazado (401)
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/me")
                .header("Authorization", "Bearer " + priorToken))
            .andExpect(status().isUnauthorized());

        // 2. Nuevo login es rechazado con 403 account_disabled
        String loginPayload = """
            {
                "email": "%s",
                "password": "%s"
            }
            """.formatted(user.getEmail(), password);

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("account_disabled"));
    }

    @Test
    void adminCannotDeactivateSelf() throws Exception {
        String payload = """
            {
                "enabled": false
            }
            """;

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/" + adminUser.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("self_deactivation_not_allowed"));
    }

    @Test
    void adminCannotDemoteSelf() throws Exception {
        String payload = """
            {
                "email": "%s",
                "role": "EMPLOYEE"
            }
            """.formatted(adminUser.getEmail());

        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + adminUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("self_role_change_not_allowed"));
    }

    @Test
    void employeeReceivesForbidden403OnAllAdminEndpoints() throws Exception {
        // Listar
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("forbidden"));

        // Obtener por ID
        mockMvc.perform(MockMvcRequestBuilders.get("/users/" + employeeUser.getId())
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("forbidden"));

        // Crear
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fail@example.com\",\"password\":\"Password123\",\"role\":\"EMPLOYEE\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("forbidden"));

        // Editar
        mockMvc.perform(MockMvcRequestBuilders.put("/users/" + employeeUser.getId())
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fail@example.com\",\"role\":\"EMPLOYEE\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("forbidden"));

        // Desactivar
        mockMvc.perform(MockMvcRequestBuilders.patch("/users/" + employeeUser.getId() + "/status")
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": false}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void anonymousRequestReceivesUnauthorized401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("authentication_required"));

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"Password123\",\"role\":\"EMPLOYEE\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private String extract(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\":\\\"?([^,\\\"}]+)\\\"?")
            .matcher(json);
        assertThat(matcher.find()).as("Expected field %s in %s", field, json).isTrue();
        return matcher.group(1);
    }
}
