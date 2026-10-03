package com.rexxo.otp.service;

public interface EmailProvider {
    void sendEmail(String to, String subject, String bodyText, String htmlText);
    boolean isConfigured();
}
