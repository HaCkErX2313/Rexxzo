package com.rexxo.config;

import com.rexxo.entity.Admin;
import com.rexxo.entity.User;
import com.rexxo.repository.AdminRepository;
import com.rexxo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:${ADMIN_EMAIL:}}")
    private String configuredAdminEmail;

    @Value("${app.admin.password:${ADMIN_PASSWORD:}}")
    private String configuredAdminPassword;

    @Value("${app.admin.name:${ADMIN_NAME:REXXZO Administrator}}")
    private String configuredAdminName;

    @Override
    public void run(String... args) {
        if (configuredAdminEmail == null || configuredAdminEmail.isBlank()
                || configuredAdminPassword == null || configuredAdminPassword.isBlank()) {
            log.info("No ADMIN_EMAIL / ADMIN_PASSWORD configured in environment. Admin setup available via /admin/signup.");
            return;
        }

        String normalizedEmail = configuredAdminEmail.trim().toLowerCase();
        String hashedPassword = passwordEncoder.encode(configuredAdminPassword.trim());

        adminRepository.findByEmail(normalizedEmail).ifPresentOrElse(
            admin -> {
                admin.setPasswordHash(hashedPassword);
                admin.setEnabled(true);
                if (configuredAdminName != null && !configuredAdminName.isBlank()) {
                    admin.setName(configuredAdminName.trim());
                }
                adminRepository.save(admin);
                log.info("Configured administrator password updated and enabled for: {}", normalizedEmail);
            },
            () -> {
                Admin admin = Admin.builder()
                    .name(configuredAdminName.trim())
                    .email(normalizedEmail)
                    .passwordHash(hashedPassword)
                    .role("ADMIN")
                    .enabled(true)
                    .build();
                adminRepository.save(admin);
                log.info("Configured administrator provisioned for: {}", normalizedEmail);
            }
        );

        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(
            user -> {
                user.setRole(User.Role.ADMIN);
                user.setPassword(hashedPassword);
                user.setIsActive(true);
                userRepository.save(user);
            },
            () -> {
                User user = User.builder()
                    .name(configuredAdminName.trim())
                    .email(normalizedEmail)
                    .password(hashedPassword)
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
                userRepository.save(user);
            }
        );
    }
}
