package com.tiendalaesquina.config;

import com.tiendalaesquina.application.auth.PasswordPolicy;
import com.tiendalaesquina.application.common.EmailNormalizer;
import com.tiendalaesquina.domain.model.Role;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.RoleRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap {

    private final BootstrapProperties properties;
    private final RoleRepository roles;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(BootstrapProperties properties, RoleRepository roles,
                          UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void provisionAdmin() {
        if (!PasswordPolicy.isValid(properties.getInitialPassword())) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must satisfy the password policy");
        }
        String email = EmailNormalizer.normalize(properties.getEmail());
        Role adminRole = roles.findByName(RoleName.ADMIN)
            .orElseThrow(() -> new IllegalStateException("ADMIN role is missing from database migration"));
        UserAccount admin = users.findByEmail(email).orElse(null);
        if (admin == null) {
            users.save(new UserAccount(email, passwordEncoder.encode(properties.getInitialPassword()),
                adminRole, true));
            return;
        }
        if (admin.getRole().getName() != RoleName.ADMIN) {
            admin.setRole(adminRole);
        }
        admin.verify();
        admin.enable();
        users.save(admin);
    }
}
