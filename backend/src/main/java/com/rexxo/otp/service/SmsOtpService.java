package com.rexxo.otp.service;

public interface SmsOtpService {
    void sendOtp(String phoneNumber, String otp);
}
