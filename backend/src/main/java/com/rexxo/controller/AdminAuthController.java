package com.rexxo.controller;

import com.rexxo.entity.Admin;
import com.rexxo.entity.User;
import com.rexxo.repository.AdminRepository;
import com.rexxo.repository.UserRepository;
import com.rexxo.security.JwtTokenProvider;
import com.rexxo.service.AdminAuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AdminAuditService auditService;

    // ─── 1. Setup Status Check ────────────────────────────────────────────────
    @GetMapping("/setup-status")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getSetupStatus() {
        boolean hasAdmin = adminRepository.count() > 0 || userRepository.countByRole(User.Role.ADMIN) > 0;
        return ResponseEntity.ok(Map.of(
            "setupRequired", !hasAdmin,
            "message", hasAdmin ? "Admin setup is already completed." : "Initial administrator setup required."
        ));
    }

    // ─── 2. Initial Admin Setup / Provisioning ────────────────────────────────
    @PostMapping("/setup")
    @Transactional
    public ResponseEntity<?> setupInitialAdmin(@Valid @RequestBody AdminSetupRequest request) {
        boolean hasAdmin = adminRepository.count() > 0 || userRepository.countByRole(User.Role.ADMIN) > 0;
        if (hasAdmin) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Admin setup is already completed",
                "message", "Admin setup is already completed. Please log in at /admin/login."
            ));
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        String hashedPassword = passwordEncoder.encode(request.password());

        // Create in admins table
        Admin admin = Admin.builder()
            .name(request.name().trim())
            .email(normalizedEmail)
            .passwordHash(hashedPassword)
            .role("ADMIN")
            .enabled(true)
            .build();
        adminRepository.save(admin);

        // Also ensure user table record exists for seamless entity relations
        if (!userRepository.existsByEmail(normalizedEmail)) {
            User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .password(hashedPassword)
                .role(User.Role.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build();
            userRepository.save(user);
        }

        String token = tokenProvider.generateTokenFromEmail(normalizedEmail);
        auditService.log(normalizedEmail, "ADMIN_INITIAL_SETUP", "Admin", admin.getId(),
            "Initial root administrator established", null);

        log.info("Initial administrator account established for email: {}", normalizedEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(new AdminAuthResponse(
            token, "Bearer", admin.getEmail(), admin.getName(), "ADMIN"
        ));
    }

    // ─── 3. Admin Login ───────────────────────────────────────────────────────
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // 1. Look up in admins table
        Optional<Admin> adminOpt = adminRepository.findByEmail(normalizedEmail);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (!Boolean.TRUE.equals(admin.getEnabled())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Admin account disabled",
                    "message", "Admin account is disabled. Please contact the system owner."
                ));
            }
            if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "You entered an incorrect email or password."
                ));
            }

            String token = tokenProvider.generateTokenFromEmail(admin.getEmail());
            auditService.log(admin.getEmail(), "ADMIN_LOGIN", "Admin", admin.getId(),
                "Administrator login successful", null);

            return ResponseEntity.ok(new AdminAuthResponse(
                token, "Bearer", admin.getEmail(), admin.getName(), "ADMIN"
            ));
        }

        // 2. Look up in users table (support existing/test admins)
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() != User.Role.ADMIN) {
                // Reject normal customers attempting admin login without revealing role
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "You entered an incorrect email or password."
                ));
            }
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Admin account disabled",
                    "message", "Admin account is disabled."
                ));
            }
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "You entered an incorrect email or password."
                ));
            }

            String token = tokenProvider.generateTokenFromEmail(user.getEmail());
            auditService.log(user.getEmail(), "ADMIN_LOGIN", "User", user.getId(),
                "Administrator login successful", null);

            return ResponseEntity.ok(new AdminAuthResponse(
                token, "Bearer", user.getEmail(), user.getName(), "ADMIN"
            ));
        }

        // Not found
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
            "error", "Invalid credentials",
            "message", "You entered an incorrect email or password."
        ));
    }

    // ─── 4. Current Admin Info ────────────────────────────────────────────────
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCurrentAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Unauthorized",
                "message", "Your admin session has expired. Please log in again."
            ));
        }

        String email = auth.getName();
        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "email", admin.get().getEmail(),
                "name", admin.get().getName(),
                "role", admin.get().getRole()
            ));
        }

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getRole() == User.Role.ADMIN) {
            return ResponseEntity.ok(Map.of(
                "email", user.get().getEmail(),
                "name", user.get().getName(),
                "role", "ADMIN"
            ));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "Forbidden",
            "message", "You do not have permission to access this area."
        ));
    }

    // DTO records
    public record AdminSetupRequest(
        @NotBlank(message = "Administrator name is required") String name,
        @NotBlank(message = "Administrator email is required") @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password
    ) {}

    public record AdminLoginRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Password is required") String password
    ) {}

    public record AdminAuthResponse(
        String token,
        String tokenType,
        String email,
        String name,
        String role
    ) {}
}
