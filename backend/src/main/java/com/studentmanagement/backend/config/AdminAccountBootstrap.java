package com.studentmanagement.backend.config;

import com.studentmanagement.backend.model.AccountRole;
import com.studentmanagement.backend.model.UserAccount;
import com.studentmanagement.backend.repository.UserAccountRepository;
import com.studentmanagement.backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
public class AdminAccountBootstrap implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(AdminAccountBootstrap.class);

    private final String username;
    private final String password;
    private final boolean accountResetEnabled;
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountBootstrap(
            @Value("${app.auth.admin.username:}") String email,
            @Value("${app.auth.admin.password:}") String password,
            @Value("${account.reset.enabled:false}") boolean accountResetEnabled,
            UserAccountRepository accounts,
            PasswordEncoder passwordEncoder) {
        this.username = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
        this.accountResetEnabled = accountResetEnabled;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountResetEnabled) {
            logger.info("Admin account bootstrap skipped during account password reset.");
            return;
        }
        if (username.isEmpty() && password.isEmpty()) {
            logger.warn("No admin bootstrap credentials configured; no bootstrap account was created.");
            return;
        }
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (username.isEmpty() || passwordBytes < 12 || passwordBytes > 72) {
            throw new IllegalStateException(
                    "Configure both ADMIN_EMAIL and ADMIN_PASSWORD; the password must contain 12-72 UTF-8 bytes.");
        }

        String normalizedUsername = AuthService.normalizeEmail(username);
        var existing = accounts.findByEmailIgnoreCase(normalizedUsername);
        if (existing.isPresent()) {
            if (existing.get().getRole() != AccountRole.ADMIN) {
                throw new IllegalStateException("The configured admin username is already assigned to a non-admin account.");
            }
            return;
        }

        accounts.save(new UserAccount(normalizedUsername, passwordEncoder.encode(password), AccountRole.ADMIN, null));
    }
}
