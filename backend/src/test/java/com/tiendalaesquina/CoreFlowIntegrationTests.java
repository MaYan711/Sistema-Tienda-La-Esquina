package com.tiendalaesquina;

import com.tiendalaesquina.application.mail.EmailService;
import com.tiendalaesquina.domain.model.OtpChallenge;
import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.repository.OtpChallengeRepository;
import com.tiendalaesquina.domain.repository.RoleRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.tiendalaesquina.application.common.EmailNormalizer;
import com.tiendalaesquina.config.BootstrapProperties;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.context.SpringBootTest
@AutoConfigureMockMvc
@Import(CoreFlowIntegrationTests.TestMailConfiguration.class)
class CoreFlowIntegrationTests {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapProperties bootstrapProperties;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private OtpChallengeRepository challenges;

    @Autowired
    private TestMailService mail;

    @BeforeEach
    void cleanTestAccounts() {
        challenges.deleteAll();
        users.findAll().stream()
                .filter(user -> user.getEmail().startsWith("core-"))
                .filter(user -> user.getEmail().endsWith("@example.com"))
                .forEach(users::delete);
        mail.clear();
    }

    @Test
    void migrationsSeedSingleRoleCatalogAndBootstrapAdminAsHash() {
        var allRoles = roles.findAll();

        assertThat(allRoles)
                .extracting(role -> role.getName().name())
                .contains("ADMIN", "EMPLOYEE");

        var adminRole = roles.findByName(RoleName.ADMIN)
                .orElseThrow(() ->
                        new AssertionError("No existe el rol ADMIN en la base de datos"));

        assertThat(adminRole.getDescription())
                .isEqualTo("Administrador del sistema");

        assertThat(bootstrapProperties)
                .as("BootstrapProperties debe estar inyectado")
                .isNotNull();

        String adminEmail =
                EmailNormalizer.normalize(bootstrapProperties.getEmail());

        assertThat(adminEmail)
                .as("El correo del administrador bootstrap debe estar configurado")
                .isNotBlank();

        var admin = users.findByEmail(adminEmail)
                .orElseThrow(() ->
                        new AssertionError(
                                "No existe el administrador bootstrap con correo: " + adminEmail
                        ));

        assertThat(admin.getRole().getName()).isEqualTo(RoleName.ADMIN);
        assertThat(admin.isVerified()).isTrue();
        assertThat(admin.getPasswordHash()).startsWith("$2");
    }
    @Test
    void openApiProblemResponsesReferenceDeclaredSchema() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists())
            .andExpect(jsonPath("$.paths['/auth/register'].post.responses['400']"
                + ".content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ProblemDetail"));
    }

    @Test
    void registrationOtpIsPurposeBoundSingleUseAndEnablesLogin() throws Exception {
        String email = uniqueEmail();
        MvcResult registration = mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.challengeId").isNotEmpty())
            .andReturn();

        UUID challengeId = UUID.fromString(extract(registration, "challengeId"));
        assertThat(mail.last().purpose()).isEqualTo(OtpPurpose.REGISTRATION);
        OtpChallenge stored = challenges.findById(challengeId).orElseThrow();
        assertThat(stored.getCodeHash()).isNotEqualTo(mail.last().code());

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"challengeId\":\""
                    + challengeId + "\",\"otp\":\"111111\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("invalid_otp"));
        assertThat(challenges.findById(challengeId).orElseThrow().getAttempts()).isEqualTo(1);

        String code = mail.last().code();
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"challengeId\":\""
                    + challengeId + "\",\"otp\":\"" + code + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("La cuenta fue verificada correctamente"));

        String login = mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String token = extract(login, "accessToken");

        mockMvc.perform(MockMvcRequestBuilders.get("/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value(RoleName.EMPLOYEE.name()));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"challengeId\":\""
                    + challengeId + "\",\"otp\":\"" + code + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("account_already_verified"));
    }

    @Test
    void recoveryOtpChangesPasswordOnceAndDoesNotExposeUnknownAccounts() throws Exception {
        String email = createVerifiedUser();

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message")
                .value("Si el correo está registrado, recibirá un código de recuperación"));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/password-recovery")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
            .andExpect(status().isAccepted());
        assertThat(mail.last().purpose()).isEqualTo(OtpPurpose.PASSWORD_RECOVERY);
        String code = mail.last().code();

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + code
                    + "\",\"newPassword\":\"NewPassword123\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + code
                    + "\",\"newPassword\":\"AnotherPassword123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid_otp"));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"NewPassword123\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void twoFactorLoginAndRoleAuthorizationAreEnforced() throws Exception {
        String email = createVerifiedUser();
        String token = login(email, "Password123");

        MvcResult enable = mockMvc.perform(MockMvcRequestBuilders.post("/auth/2fa/enable")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Password123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        String enableChallenge = extract(enable, "challengeId");
        String enableCode = mail.last().code();

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/2fa/enable/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"challengeId\":\"" + enableChallenge + "\",\"otp\":\""
                    + enableCode + "\"}"))
            .andExpect(status().isOk());

        MvcResult loginChallenge = mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(jsonPath("$.requiresTwoFactor").value(true))
            .andReturn();
        String loginChallengeId = extract(loginChallenge, "challengeId");
        String loginCode = mail.last().code();
        String twoFactorToken = extract(mockMvc.perform(MockMvcRequestBuilders.post("/auth/login/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"challengeId\":\"" + loginChallengeId + "\",\"otp\":\""
                    + loginCode + "\"}"))
            .andExpect(status().isOk())
            .andReturn(), "accessToken");
        assertThat(twoFactorToken).isNotBlank();

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/ping")
                .header("Authorization", "Bearer " + twoFactorToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void missingAndMalformedJwtReturnProblemDetails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("authentication_required"));

        mockMvc.perform(MockMvcRequestBuilders.get("/auth/me")
                .header("Authorization", "Bearer malformed-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("invalid_token"));
    }

    private String createVerifiedUser() throws Exception {
        String email = uniqueEmail();
        MvcResult registration = mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String challengeId = extract(registration, "challengeId");
        String code = mail.last().code();
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"challengeId\":\""
                    + challengeId + "\",\"otp\":\"" + code + "\"}"))
            .andExpect(status().isOk());
        return email;
    }

    private String login(String email, String password) throws Exception {
        return extract(mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn(), "accessToken");
    }

    private String uniqueEmail() {
        return "core-" + UUID.randomUUID() + "@example.com";
    }

    private String extract(MvcResult result, String field) throws Exception {
        return extract(result.getResponse().getContentAsString(), field);
    }

    private String extract(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\":\\\"([^\\\"]+)\\\"")
            .matcher(json);
        assertThat(matcher.find()).as("Expected JSON field %s in %s", field, json).isTrue();
        return matcher.group(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestMailConfiguration {

        @Bean
        @Primary
        TestMailService emailService() {
            return new TestMailService();
        }
    }

    static class TestMailService implements EmailService {

        private final AtomicReference<SentOtp> latest = new AtomicReference<>();

        @Override
        public void sendOtp(String recipient, OtpPurpose purpose, String code, Instant expiresAt) {
            latest.set(new SentOtp(recipient, purpose, code, expiresAt));
        }

        SentOtp last() {
            SentOtp sent = latest.get();
            assertThat(sent).isNotNull();
            return sent;
        }

        void clear() {
            latest.set(null);
        }
    }

    record SentOtp(String recipient, OtpPurpose purpose, String code, Instant expiresAt) {
    }
}
