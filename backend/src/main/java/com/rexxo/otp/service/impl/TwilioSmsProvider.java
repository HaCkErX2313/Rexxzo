package com.rexxo.otp.service.impl;

import com.rexxo.otp.config.OtpConfig;
import com.rexxo.otp.service.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class TwilioSmsProvider implements SmsProvider {

    private final OtpConfig otpConfig;
    private final WebClient webClient;

    public TwilioSmsProvider(OtpConfig otpConfig) {
        this.otpConfig = otpConfig;
        this.webClient = WebClient.builder()
            .baseUrl("https://api.twilio.com/2010-04-01")
            .build();
    }

    @Override
    public String getProviderName() {
        return "twilio";
    }

    @Override
    public boolean isConfigured() {
        return otpConfig.getSmsApiKey() != null && !otpConfig.getSmsApiKey().isBlank()
            && otpConfig.getSmsApiSecret() != null && !otpConfig.getSmsApiSecret().isBlank();
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!isConfigured()) {
            if (otpConfig.isDevMode()) {
                log.info("[DEV MODE] SMS to: {} (Twilio not configured)", maskPhone(phoneNumber));
                return;
            }
            log.warn("Twilio SMS provider is not configured. Set SMS_API_KEY and SMS_API_SECRET in backend/.env.");
            throw new IllegalStateException("SMS delivery service is currently not configured on this server.");
        }

        try {
            String accountSid = otpConfig.getSmsApiKey();
            String authToken = otpConfig.getSmsApiSecret();
            String authHeader = "Basic " + Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            // E.164 phone formatting
            String toPhone = phoneNumber.startsWith("+") ? phoneNumber : "+91" + phoneNumber.replaceAll("[^0-9]", "");

            webClient.post()
                .uri("/Accounts/{AccountSid}/Messages.json", accountSid)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData("To", toPhone)
                    .with("From", otpConfig.getSmsSenderId())
                    .with("Body", message))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.info("SMS dispatched via Twilio to: {}", maskPhone(phoneNumber));
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to {}: {}", maskPhone(phoneNumber), e.getMessage());
            throw new RuntimeException("Failed to send verification SMS. Please try again later.", e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, Math.min(3, phone.length())) + "****" + phone.substring(Math.max(0, phone.length() - 4));
    }
}
