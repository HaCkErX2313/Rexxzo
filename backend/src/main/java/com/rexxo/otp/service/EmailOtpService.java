package com.rexxo.otp.service;

public interface EmailOtpService {
    void sendOtp(String email, String otp);
}
