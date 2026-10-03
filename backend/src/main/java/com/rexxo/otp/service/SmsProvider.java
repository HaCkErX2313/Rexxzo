package com.rexxo.otp.service;

public interface SmsProvider {
    void sendSms(String phoneNumber, String message);
    boolean isConfigured();
    String getProviderName();
}
