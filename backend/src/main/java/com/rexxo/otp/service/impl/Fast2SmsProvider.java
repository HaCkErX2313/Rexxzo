package com.rexxo.otp.service.impl;

import com.rexxo.otp.config.OtpConfig;
import com.rexxo.otp.service.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class Fast2SmsProvider implements SmsProvider {

    private final OtpConfig otpConfig;
    private final WebClient webClient;

    public Fast2SmsProvider(OtpConfig otpConfig) {
        this.otpConfig = otpConfig;
        this.webClient = WebClient.builder()
            .baseUrl("https://www.fast2sms.com/dev")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public String getProviderName() {
        return "fast2sms";
    }

    @Override
    public boolean isConfigured() {
        return otpConfig.getSmsApiKey() != null && !otpConfig.getSmsApiKey().isBlank();
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!isConfigured()) {
            if (otpConfig.isDevMode()) {
                log.info("[DEV MODE] SMS to: {} (Fast2SMS API key not configured)", maskPhone(phoneNumber));
                return;
            }
            log.warn("Fast2SMS provider is not configured. Set SMS_API_KEY in backend/.env.");
            throw new IllegalStateException("SMS delivery service is currently not configured on this server.");
        }

        try {
            // Strip leading +91 or + for Fast2SMS (expects 10-digit number)
            String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
            if (cleanNumber.startsWith("91") && cleanNumber.length() == 12) {
                cleanNumber = cleanNumber.substring(2);
            }

            Map<String, Object> payload = Map.of(
                "route", "q",
                "message", message,
                "language", "english",
                "flash", 0,
                "numbers", cleanNumber
            );

            String response = webClient.post()
                .uri("/bulkV2")
                .header("authorization", otpConfig.getSmsApiKey())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.info("SMS dispatched via Fast2SMS to: {}", maskPhone(phoneNumber));
        } catch (Exception e) {
            log.error("Failed to send SMS via Fast2SMS to {}: {}", maskPhone(phoneNumber), e.getMessage());
            throw new RuntimeException("Failed to send verification SMS. Please try again later.", e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, Math.min(3, phone.length())) + "****" + phone.substring(Math.max(0, phone.length() - 4));
    }
}
