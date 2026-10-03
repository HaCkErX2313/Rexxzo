package com.rexxo.otp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter @Setter
public class OtpConfig {

    @Value("${otp.expiration-minutes:5}")
    private int expirationMinutes;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${otp.dev-mode:false}")
    private boolean devMode;

    @Value("${mail.from:noreply@rexxzo.com}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${sms.provider:fast2sms}")
    private String smsProvider;

    @Value("${sms.api-key:}")
    private String smsApiKey;

    @Value("${sms.api-secret:}")
    private String smsApiSecret;

    @Value("${sms.sender-id:REXXZO}")
    private String smsSenderId;
}
