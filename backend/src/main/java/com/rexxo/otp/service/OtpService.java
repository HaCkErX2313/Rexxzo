package com.rexxo.otp.service;

import com.rexxo.dto.auth.SignupInitiateRequest;
import com.rexxo.dto.auth.SignupInitiateResponse;
import com.rexxo.dto.auth.SignupVerifyRequest;
import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.otp.config.OtpConfig;
import com.rexxo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile("^(\\+91|91|0)?[6-9]\\d{9}$");

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final EmailOtpService emailOtpService;
    private final SmsOtpService smsOtpService;
    private final PasswordEncoder passwordEncoder;
    private final OtpConfig otpConfig;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public SignupInitiateResponse initiateSignup(SignupInitiateRequest request) {
        // 1. Validate fields
        if (request.fullName() == null || request.fullName().trim().length() < 2) {
            throw RexxoException.badRequest("Full name must be at least 2 characters.");
        }
        if (request.email() == null || !EMAIL_PATTERN.matcher(request.email().trim()).matches()) {
            throw RexxoException.badRequest("Please enter a valid email address.");
        }
        String normalizedPhone = normalizeIndianPhone(request.phone());
        if (normalizedPhone == null) {
            throw RexxoException.badRequest("Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw RexxoException.badRequest("Password must be at least 8 characters long.");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw RexxoException.badRequest("Passwords do not match.");
        }

        String email = request.email().toLowerCase().trim();

        // 2. Check if email or phone already belongs to an existing user
        if (userRepository.existsByEmail(email)) {
            throw RexxoException.conflict("Email is already registered. Please sign in.");
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw RexxoException.conflict("Mobile number is already registered. Please sign in.");
        }

        // 3. Invalidate previous pending registrations for this email/phone to prevent replay/stale sessions
        pendingRegistrationRepository.findTopByEmailOrderByCreatedAtDesc(email).ifPresent(old -> {
            invalidateSessionOtps(old.getVerificationId());
            pendingRegistrationRepository.delete(old);
        });
        pendingRegistrationRepository.findTopByPhoneOrderByCreatedAtDesc(normalizedPhone).ifPresent(old -> {
            invalidateSessionOtps(old.getVerificationId());
            pendingRegistrationRepository.delete(old);
        });

        // 4. Generate cryptographically secure 6-digit OTPs
        String emailOtp = generate6DigitOtp();
        String phoneOtp = generate6DigitOtp();

        // 5. Hash OTPs with BCrypt before database storage (never plaintext!)
        String hashedEmailOtp = passwordEncoder.encode(emailOtp);
        String hashedPhoneOtp = passwordEncoder.encode(phoneOtp);

        // 6. Hash password with BCrypt
        String passwordHash = passwordEncoder.encode(request.password());

        // 7. Store temporary registration session
        String verificationId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpConfig.getExpirationMinutes());

        PendingRegistration pending = PendingRegistration.builder()
            .verificationId(verificationId)
            .fullName(request.fullName().trim())
            .email(email)
            .phone(normalizedPhone)
            .passwordHash(passwordHash)
            .emailVerified(false)
            .phoneVerified(false)
            .expiresAt(expiresAt)
            .build();
        pendingRegistrationRepository.save(pending);

        // 8. Store OtpVerification records
        OtpVerification emailVerification = OtpVerification.builder()
            .sessionId(verificationId)
            .identifier(email)
            .identifierType(OtpVerification.IdentifierType.EMAIL)
            .otpHash(hashedEmailOtp)
            .purpose(OtpVerification.Purpose.SIGNUP)
            .expiresAt(expiresAt)
            .maxAttempts(otpConfig.getMaxAttempts())
            .attemptCount(0)
            .lastSentAt(LocalDateTime.now())
            .build();
        otpVerificationRepository.save(emailVerification);

        OtpVerification phoneVerification = OtpVerification.builder()
            .sessionId(verificationId)
            .identifier(normalizedPhone)
            .identifierType(OtpVerification.IdentifierType.PHONE)
            .otpHash(hashedPhoneOtp)
            .purpose(OtpVerification.Purpose.SIGNUP)
            .expiresAt(expiresAt)
            .maxAttempts(otpConfig.getMaxAttempts())
            .attemptCount(0)
            .lastSentAt(LocalDateTime.now())
            .build();
        otpVerificationRepository.save(phoneVerification);

        // 9. Dispatch OTPs to email and SMS
        try {
            emailOtpService.sendOtp(email, emailOtp);
        } catch (IllegalStateException e) {
            log.error("Email OTP service unavailable: {}", e.getMessage());
            if (!otpConfig.isDevMode()) {
                throw RexxoException.badRequest("Email delivery service is currently not configured on this server. Please configure SMTP credentials (MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD).");
            }
        } catch (Exception e) {
            log.error("Email OTP dispatch failed: {}", e.getMessage());
            if (!otpConfig.isDevMode()) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Failed to deliver email verification code.";
                throw RexxoException.badRequest(errorMsg);
            }
        }

        try {
            smsOtpService.sendOtp(normalizedPhone, phoneOtp);
        } catch (Exception e) {
            log.warn("SMS OTP dispatch failed: {}", e.getMessage());
            if (!otpConfig.isDevMode()) {
                throw RexxoException.badRequest("Failed to send mobile verification SMS. Please check your mobile number.");
            }
        }

        return new SignupInitiateResponse(
            true,
            "Verification codes sent to your email and mobile number.",
            verificationId,
            maskEmail(email),
            maskPhone(normalizedPhone)
        );
    }

    @Transactional(noRollbackFor = {RexxoException.class})
    public User verifyAndCreateUser(SignupVerifyRequest request) {
        PendingRegistration pending = pendingRegistrationRepository.findByVerificationId(request.verificationId())
            .orElseThrow(() -> RexxoException.badRequest("Invalid or expired verification session. Please start signup again."));

        if (pending.isExpired()) {
            throw RexxoException.badRequest("OTP has expired. Please request a new OTP.");
        }

        OtpVerification emailOtpRec = otpVerificationRepository
            .findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
                request.verificationId(),
                OtpVerification.IdentifierType.EMAIL,
                OtpVerification.Purpose.SIGNUP
            )
            .orElseThrow(() -> RexxoException.badRequest("Email verification record not found."));

        OtpVerification phoneOtpRec = otpVerificationRepository
            .findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
                request.verificationId(),
                OtpVerification.IdentifierType.PHONE,
                OtpVerification.Purpose.SIGNUP
            )
            .orElseThrow(() -> RexxoException.badRequest("Mobile verification record not found."));

        // Check expiration
        if (emailOtpRec.isExpired() || phoneOtpRec.isExpired()) {
            throw RexxoException.badRequest("OTP has expired. Please request a new OTP.");
        }

        // Check maximum attempts limit
        if (emailOtpRec.hasExceededAttempts() || phoneOtpRec.hasExceededAttempts() || emailOtpRec.isExpired() || phoneOtpRec.isExpired()) {
            throw RexxoException.badRequest("Too many incorrect attempts. Please request a new OTP.");
        }

        // Increment attempt count on each submission
        int emailAttempts = (emailOtpRec.getAttemptCount() == null ? 0 : emailOtpRec.getAttemptCount()) + 1;
        int phoneAttempts = (phoneOtpRec.getAttemptCount() == null ? 0 : phoneOtpRec.getAttemptCount()) + 1;
        emailOtpRec.setAttemptCount(emailAttempts);
        phoneOtpRec.setAttemptCount(phoneAttempts);

        // Verify hashes
        boolean emailMatch = passwordEncoder.matches(request.emailOtp().trim(), emailOtpRec.getOtpHash());
        boolean phoneMatch = passwordEncoder.matches(request.phoneOtp().trim(), phoneOtpRec.getOtpHash());

        // If either failed, check if max attempts reached
        if (!emailMatch || !phoneMatch) {
            boolean reachedMax = emailAttempts >= emailOtpRec.getMaxAttempts() || phoneAttempts >= phoneOtpRec.getMaxAttempts();
            if (reachedMax) {
                // Invalidate OTP immediately
                emailOtpRec.setExpiresAt(LocalDateTime.now().minusSeconds(1));
                phoneOtpRec.setExpiresAt(LocalDateTime.now().minusSeconds(1));
                otpVerificationRepository.save(emailOtpRec);
                otpVerificationRepository.save(phoneOtpRec);
                throw RexxoException.badRequest("Too many incorrect attempts. Please request a new OTP.");
            }

            otpVerificationRepository.save(emailOtpRec);
            otpVerificationRepository.save(phoneOtpRec);

            if (!emailMatch && !phoneMatch) {
                throw RexxoException.unauthorized("Incorrect email and mobile verification codes.");
            }
            if (!emailMatch) {
                throw RexxoException.unauthorized("Incorrect email verification code.");
            }
            throw RexxoException.unauthorized("Incorrect mobile verification code.");
        }

        // Race condition check: ensure neither email nor phone was taken while user was typing OTPs
        if (userRepository.existsByEmail(pending.getEmail())) {
            throw RexxoException.conflict("Email is already registered. Please sign in.");
        }
        if (userRepository.existsByPhone(pending.getPhone())) {
            throw RexxoException.conflict("Mobile number is already registered. Please sign in.");
        }

        // ONLY NOW create the permanent user account in PostgreSQL
        User user = User.builder()
            .name(pending.getFullName())
            .email(pending.getEmail())
            .phone(pending.getPhone())
            .password(pending.getPasswordHash()) // already BCrypt encoded
            .role(User.Role.CUSTOMER)
            .isActive(true)
            .emailVerified(true)
            .phoneVerified(true)
            .build();
        user = userRepository.save(user);

        // Provision Cart and Wishlist
        Cart cart = Cart.builder().user(user).build();
        cartRepository.save(cart);

        Wishlist wishlist = Wishlist.builder().user(user).build();
        wishlistRepository.save(wishlist);

        // Mark OTP records verified and clean up pending registration
        emailOtpRec.setVerifiedAt(LocalDateTime.now());
        phoneOtpRec.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepository.save(emailOtpRec);
        otpVerificationRepository.save(phoneOtpRec);

        pendingRegistrationRepository.delete(pending);

        log.info("Permanent user account created successfully for: {}", maskEmail(user.getEmail()));
        return user;
    }

    @Transactional
    public void resendEmailOtp(String verificationId) {
        PendingRegistration pending = pendingRegistrationRepository.findByVerificationId(verificationId)
            .orElseThrow(() -> RexxoException.badRequest("Invalid or expired verification session."));

        if (pending.isExpired()) {
            throw RexxoException.badRequest("Verification session has expired. Please restart registration.");
        }

        OtpVerification emailOtpRec = otpVerificationRepository
            .findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
                verificationId,
                OtpVerification.IdentifierType.EMAIL,
                OtpVerification.Purpose.SIGNUP
            )
            .orElseThrow(() -> RexxoException.badRequest("Email verification record not found."));

        // Cooldown check (60s)
        long secondsSince = ChronoUnit.SECONDS.between(emailOtpRec.getLastSentAt(), LocalDateTime.now());
        if (secondsSince < otpConfig.getResendCooldownSeconds()) {
            long remaining = otpConfig.getResendCooldownSeconds() - secondsSince;
            throw RexxoException.tooManyRequests("Please wait " + remaining + " seconds before requesting another email code.");
        }

        String newOtp = generate6DigitOtp();
        String newHash = passwordEncoder.encode(newOtp);

        emailOtpRec.setOtpHash(newHash);
        emailOtpRec.setAttemptCount(0);
        emailOtpRec.setExpiresAt(LocalDateTime.now().plusMinutes(otpConfig.getExpirationMinutes()));
        emailOtpRec.setLastSentAt(LocalDateTime.now());
        otpVerificationRepository.save(emailOtpRec);

        try {
            emailOtpService.sendOtp(pending.getEmail(), newOtp);
        } catch (IllegalStateException e) {
            log.error("Email OTP resend failed - service not configured: {}", e.getMessage());
            throw RexxoException.badRequest("Email delivery service is currently not configured on this server. Please configure SMTP credentials.");
        } catch (Exception e) {
            log.error("Email OTP resend failed: {}", e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Failed to resend email verification code.";
            throw RexxoException.badRequest(errorMsg);
        }
    }

    @Transactional
    public void resendPhoneOtp(String verificationId) {
        PendingRegistration pending = pendingRegistrationRepository.findByVerificationId(verificationId)
            .orElseThrow(() -> RexxoException.badRequest("Invalid or expired verification session."));

        if (pending.isExpired()) {
            throw RexxoException.badRequest("Verification session has expired. Please restart registration.");
        }

        OtpVerification phoneOtpRec = otpVerificationRepository
            .findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
                verificationId,
                OtpVerification.IdentifierType.PHONE,
                OtpVerification.Purpose.SIGNUP
            )
            .orElseThrow(() -> RexxoException.badRequest("Mobile verification record not found."));

        // Cooldown check (60s)
        long secondsSince = ChronoUnit.SECONDS.between(phoneOtpRec.getLastSentAt(), LocalDateTime.now());
        if (secondsSince < otpConfig.getResendCooldownSeconds()) {
            long remaining = otpConfig.getResendCooldownSeconds() - secondsSince;
            throw RexxoException.tooManyRequests("Please wait " + remaining + " seconds before requesting another mobile code.");
        }

        String newOtp = generate6DigitOtp();
        String newHash = passwordEncoder.encode(newOtp);

        phoneOtpRec.setOtpHash(newHash);
        phoneOtpRec.setAttemptCount(0);
        phoneOtpRec.setExpiresAt(LocalDateTime.now().plusMinutes(otpConfig.getExpirationMinutes()));
        phoneOtpRec.setLastSentAt(LocalDateTime.now());
        otpVerificationRepository.save(phoneOtpRec);

        smsOtpService.sendOtp(pending.getPhone(), newOtp);
    }

    @Transactional
    public void resendBothOtps(String verificationId) {
        resendEmailOtp(verificationId);
        resendPhoneOtp(verificationId);
    }

    private void invalidateSessionOtps(String sessionId) {
        if (sessionId == null) return;
        otpVerificationRepository.findBySessionId(sessionId).forEach(otp -> {
            otp.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            otpVerificationRepository.save(otp);
        });
    }

    private String generate6DigitOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    public static String normalizeIndianPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10 && (digits.startsWith("6") || digits.startsWith("7") || digits.startsWith("8") || digits.startsWith("9"))) {
            return "+91" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            String sub = digits.substring(1);
            if (sub.startsWith("6") || sub.startsWith("7") || sub.startsWith("8") || sub.startsWith("9")) {
                return "+91" + sub;
            }
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            String sub = digits.substring(2);
            if (sub.startsWith("6") || sub.startsWith("7") || sub.startsWith("8") || sub.startsWith("9")) {
                return "+91" + sub;
            }
        }
        return null;
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****@****";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "****@" + domain;
        }
        return name.charAt(0) + "****" + name.charAt(name.length() - 1) + "@" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "+91 ******0000";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "+91 ******0000";
        String last4 = digits.substring(digits.length() - 4);
        return "+91 ******" + last4;
    }
}
