package com.rexxo.dto.auth;

public record SignupInitiateResponse(
    boolean success,
    String message,
    String verificationId,
    String maskedEmail,
    String maskedPhone
) {}
