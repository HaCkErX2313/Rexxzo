package com.rexxo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_identifier_purpose", columnList = "identifier, purpose, identifier_type"),
    @Index(name = "idx_otp_session_id", columnList = "session_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(nullable = false, length = 150)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 20)
    private IdentifierType identifierType;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Purpose purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum IdentifierType {
        EMAIL, PHONE
    }

    public enum Purpose {
        SIGNUP, LOGIN, PASSWORD_RESET, CHANGE_EMAIL, CHANGE_PHONE
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean hasExceededAttempts() {
        return attemptCount != null && attemptCount >= maxAttempts;
    }
}
