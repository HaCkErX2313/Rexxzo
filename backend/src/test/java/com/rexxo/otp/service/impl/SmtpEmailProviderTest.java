package com.rexxo.otp.service.impl;

import com.rexxo.otp.config.OtpConfig;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpEmailProviderTest {

    private OtpConfig otpConfig;
    private JavaMailSender mailSender;
    private SmtpEmailProvider provider;

    @BeforeEach
    void setUp() {
        otpConfig = new OtpConfig();
        otpConfig.setMailFrom("noreply@rexxo.com");
        mailSender = mock(JavaMailSender.class);
    }

    @Test
    @DisplayName("Should report not configured when mailHost is empty or mailSender is null")
    void testNotConfiguredWhenHostEmpty() {
        otpConfig.setMailHost("");
        provider = new SmtpEmailProvider(otpConfig, mailSender);
        assertFalse(provider.isConfigured());

        otpConfig.setMailHost(null);
        provider = new SmtpEmailProvider(otpConfig, mailSender);
        assertFalse(provider.isConfigured());

        otpConfig.setMailHost("smtp.gmail.com");
        provider = new SmtpEmailProvider(otpConfig, null);
        assertFalse(provider.isConfigured());
    }

    @Test
    @DisplayName("Should report configured when valid host and mailSender are present")
    void testConfiguredWhenHostPresent() {
        otpConfig.setMailHost("smtp.gmail.com");
        provider = new SmtpEmailProvider(otpConfig, mailSender);
        assertTrue(provider.isConfigured());
    }

    @Test
    @DisplayName("Should throw IllegalStateException with clear configuration instructions when unconfigured")
    void testSendEmailWhenUnconfigured() {
        otpConfig.setMailHost("");
        provider = new SmtpEmailProvider(otpConfig, mailSender);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            provider.sendEmail("user@example.com", "Subject", "Body", null);
        });

        assertTrue(ex.getMessage().contains("Email delivery service is currently not configured"));
    }

    @Test
    @DisplayName("Should successfully send email when configured")
    void testSendEmailSuccess() {
        otpConfig.setMailHost("smtp.gmail.com");
        provider = new SmtpEmailProvider(otpConfig, mailSender);

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> {
            provider.sendEmail("user@example.com", "Test Subject", "Test Body", "<p>Test</p>");
        });

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Should sanitize MailAuthenticationException and not leak credentials")
    void testMailAuthenticationExceptionSanitized() {
        otpConfig.setMailHost("smtp.gmail.com");
        provider = new SmtpEmailProvider(otpConfig, mailSender);

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("Authentication failed 535 5.7.8"))
            .when(mailSender).send(mimeMessage);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            provider.sendEmail("user@example.com", "Test Subject", "Test Body", null);
        });

        assertTrue(ex.getMessage().contains("Email delivery authentication failed"));
        assertFalse(ex.getMessage().contains("535 5.7.8")); // Raw protocol code not exposed in top message
    }
}
