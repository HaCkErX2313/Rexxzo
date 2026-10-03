package com.rexxo.repository;

import com.rexxo.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
        String sessionId,
        OtpVerification.IdentifierType identifierType,
        OtpVerification.Purpose purpose
    );

    Optional<OtpVerification> findTopByIdentifierAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
        String identifier,
        OtpVerification.IdentifierType identifierType,
        OtpVerification.Purpose purpose
    );

    List<OtpVerification> findBySessionId(String sessionId);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
}
