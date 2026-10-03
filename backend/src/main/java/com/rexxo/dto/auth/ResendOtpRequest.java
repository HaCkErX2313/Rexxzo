package com.rexxo.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
    @NotBlank(message = "Verification ID is required")
    String verificationId
) {}
