package com.studentmanagement.backend.config;

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
public class AccountPasswordResetBootstrap implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(AccountPasswordResetBootstrap.class);

    private final boolean enabled;
    private final String email;
    private final String password;
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountPasswordResetBootstrap(
            @Value("${account.reset.enabled:false}") boolean enabled,
            @Value("${account.reset.email:}") String email,
            @Value("${account.reset.password:}") String password,
            UserAccountRepository accounts,
            PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.email = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            if (!email.isEmpty() || !password.isEmpty()) {
                throw new IllegalStateException(
                        "Set ACCOUNT_RESET_ENABLED=true to apply the requested account password reset.");
            }
            return;
        }

        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (email.isEmpty() || passwordBytes < 12 || passwordBytes > 72) {
            throw new IllegalStateException(
                    "Set ACCOUNT_RESET_EMAIL and ACCOUNT_RESET_PASSWORD; the password must contain 12-72 UTF-8 bytes.");
        }

        UserAccount account = accounts.findByEmailIgnoreCase(AuthService.normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException("The account password reset target was not found."));
        account.setPasswordHash(passwordEncoder.encode(password));
        accounts.save(account);
        logger.info("Password reset completed for {} account.", account.getRole().name().toLowerCase());
    }
}
