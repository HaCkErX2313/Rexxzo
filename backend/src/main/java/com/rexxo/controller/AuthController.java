package com.rexxo.controller;

import com.rexxo.dto.auth.*;
import com.rexxo.entity.User;
import com.rexxo.exception.RexxoException;
import com.rexxo.otp.service.OtpService;
import com.rexxo.repository.UserRepository;
import com.rexxo.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import com.rexxo.entity.Cart;
import com.rexxo.entity.Wishlist;
import com.rexxo.repository.CartRepository;
import com.rexxo.repository.WishlistRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;

    /**
     * STEP 1 (Optional/Secondary): Initiate signup with email OTP and SMS OTP.
     * Retained for future use / independent 2FA workflows.
     */
    @PostMapping("/signup/initiate")
    public ResponseEntity<SignupInitiateResponse> initiateSignup(@Valid @RequestBody SignupInitiateRequest request) {
        SignupInitiateResponse response = otpService.initiateSignup(request);
        return ResponseEntity.ok(response);
    }

    /**
     * STEP 2 (Optional/Secondary): Verify email and mobile OTP.
     * Retained for future use / independent 2FA workflows.
     */
    @PostMapping("/signup/verify")
    public ResponseEntity<AuthResponse> verifySignup(@Valid @RequestBody SignupVerifyRequest request) {
        User user = otpService.verifyAndCreateUser(request);

        // Generate JWT token for immediate authenticated session
        String token = tokenProvider.generateTokenFromEmail(user.getEmail());

        return ResponseEntity.ok(new AuthResponse(token, toUserDto(user)));
    }

    /**
     * Resend email OTP with 60s cooldown and attempt reset.
     */
    @PostMapping("/signup/resend-email-otp")
    public ResponseEntity<ApiResponseMessage> resendEmailOtp(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resendEmailOtp(request.verificationId());
        return ResponseEntity.ok(ApiResponseMessage.of(true, "New email verification code sent."));
    }

    /**
     * Resend mobile OTP with 60s cooldown and attempt reset.
     */
    @PostMapping("/signup/resend-phone-otp")
    public ResponseEntity<ApiResponseMessage> resendPhoneOtp(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resendPhoneOtp(request.verificationId());
        return ResponseEntity.ok(ApiResponseMessage.of(true, "New mobile verification code sent."));
    }

    /**
     * Resend both verification codes.
     */
    @PostMapping("/signup/resend-otp")
    public ResponseEntity<ApiResponseMessage> resendBothOtps(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resendBothOtps(request.verificationId());
        return ResponseEntity.ok(ApiResponseMessage.of(true, "New verification codes sent to your email and mobile number."));
    }

    /**
     * Direct registration flow without OTP.
     * Validates input:
     * - Full name: 2-100 characters
     * - Email: valid format (400 if invalid)
     * - Phone: 10-digit Indian mobile (400 if invalid)
     * - Password: min 8 characters (400 if weak)
     * Checks uniqueness:
     * - Email already exists -> 409
     * - Phone already exists -> 409
     * BCrypt hashes password and creates User in PostgreSQL.
     */
    @PostMapping({"/register", "/signup"})
    @Transactional
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw RexxoException.badRequest("Registration details are required.");
        }

        // 1. Validate full name
        String name = request.resolveName();
        if (name == null || name.length() < 2 || name.length() > 100) {
            throw RexxoException.badRequest("Full name must be between 2 and 100 characters.");
        }

        // 2. Validate email format (HTTP 400)
        if (request.email() == null || request.email().isBlank()) {
            throw RexxoException.badRequest("Email is required.");
        }
        String email = request.email().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw RexxoException.badRequest("Invalid email format. Please provide a valid email address.");
        }

        // 3. Validate Indian phone format (HTTP 400)
        String normalizedPhone = validateAndNormalizeIndianPhone(request.phone());

        // 4. Validate password (HTTP 400)
        if (request.password() == null || request.password().length() < 8) {
            throw RexxoException.badRequest("Password must be at least 8 characters long.");
        }

        // 5. Duplicate checks (HTTP 409)
        if (userRepository.existsByEmail(email)) {
            throw RexxoException.conflict("Email is already registered. Please sign in.");
        }
        String raw10Digits = normalizedPhone.substring(3);
        if (userRepository.existsByPhone(normalizedPhone) || userRepository.existsByPhone(raw10Digits)) {
            throw RexxoException.conflict("Mobile number is already registered. Please sign in.");
        }

        // 6. BCrypt hash password
        String hashedPassword = passwordEncoder.encode(request.password());

        // 7. Create User in PostgreSQL
        User user = User.builder()
            .name(name)
            .email(email)
            .phone(normalizedPhone)
            .password(hashedPassword)
            .role(User.Role.CUSTOMER)
            .isActive(true)
            .emailVerified(false)
            .phoneVerified(false)
            .build();
        user = userRepository.save(user);

        // 8. Provision Cart and Wishlist
        Cart cart = Cart.builder().user(user).build();
        cartRepository.save(cart);

        Wishlist wishlist = Wishlist.builder().user(user).build();
        wishlistRepository.save(wishlist);

        // 9. Generate JWT token
        String token = tokenProvider.generateTokenFromEmail(user.getEmail());

        log.info("Direct registration successful for: {}", OtpService.maskEmail(user.getEmail()));
        return ResponseEntity.ok(new AuthResponse(token, toUserDto(user)));
    }

    private String validateAndNormalizeIndianPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw RexxoException.badRequest("Mobile number is required.");
        }
        String digits = phone.replaceAll("[^0-9]", "");
        String tenDigits = null;
        if (digits.length() == 10 && (digits.startsWith("6") || digits.startsWith("7") || digits.startsWith("8") || digits.startsWith("9"))) {
            tenDigits = digits;
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            String sub = digits.substring(1);
            if (sub.startsWith("6") || sub.startsWith("7") || sub.startsWith("8") || sub.startsWith("9")) {
                tenDigits = sub;
            }
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            String sub = digits.substring(2);
            if (sub.startsWith("6") || sub.startsWith("7") || sub.startsWith("8") || sub.startsWith("9")) {
                tenDigits = sub;
            }
        }

        if (tenDigits == null) {
            throw RexxoException.badRequest("Invalid mobile number. Please provide a valid 10-digit Indian mobile number.");
        }
        return "+91" + tenDigits;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";

        // 1. Find user by email in PostgreSQL
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> RexxoException.unauthorized("Account not found. Please sign up first."));

        // 2. Compare entered password against stored HASHED password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw RexxoException.unauthorized("Invalid email or password.");
        }

        // 3. Verify active status
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw RexxoException.unauthorized("Account is deactivated.");
        }

        // 4. Authenticate and generate JWT token
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getEmail(), request.password())
        );
        String token = tokenProvider.generateToken(auth);
        return ResponseEntity.ok(new AuthResponse(token, toUserDto(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
        return ResponseEntity.ok(toUserDto(user));
    }

    private UserDto toUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().name(),
            user.getEmailVerified(),
            user.getPhoneVerified(),
            user.getCreatedAt()
        );
    }
}
