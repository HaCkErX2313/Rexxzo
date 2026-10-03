package com.rexxo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupVerifyRequest(
    @NotBlank(message = "Verification ID is required")
    String verificationId,

    @NotBlank(message = "Email OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "Email OTP must be 6 digits")
    String emailOtp,

    @NotBlank(message = "Mobile OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "Mobile OTP must be 6 digits")
    String phoneOtp
) {}
