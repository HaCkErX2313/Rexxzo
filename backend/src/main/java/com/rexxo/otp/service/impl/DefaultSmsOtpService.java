package com.rexxo.otp.service.impl;

import com.rexxo.otp.config.OtpConfig;
import com.rexxo.otp.service.SmsOtpService;
import com.rexxo.otp.service.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSmsOtpService implements SmsOtpService {

    private final List<SmsProvider> smsProviders;
    private final OtpConfig otpConfig;

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        String message = String.format(
            "REXXZO verification code: %s. Valid for %d minutes.",
            otp, otpConfig.getExpirationMinutes()
        );

        SmsProvider selectedProvider = selectProvider();
        selectedProvider.sendSms(phoneNumber, message);
    }

    private SmsProvider selectProvider() {
        String preferred = otpConfig.getSmsProvider();
        for (SmsProvider provider : smsProviders) {
            if (provider.getProviderName().equalsIgnoreCase(preferred)) {
                return provider;
            }
        }
        // Fallback to first provider in list
        if (!smsProviders.isEmpty()) {
            return smsProviders.get(0);
        }
        throw new IllegalStateException("No SMS provider configured");
    }
}
