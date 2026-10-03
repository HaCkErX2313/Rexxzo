package com.rexxo.repository;

import com.rexxo.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByVerificationId(String verificationId);

    Optional<PendingRegistration> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<PendingRegistration> findTopByPhoneOrderByCreatedAtDesc(String phone);

    void deleteByVerificationId(String verificationId);

    @Modifying
    @Query("DELETE FROM PendingRegistration p WHERE p.expiresAt < :now")
    void deleteExpiredRegistrations(@Param("now") LocalDateTime now);
}
