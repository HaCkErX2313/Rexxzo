package com.rexxo.otp.service.impl;

import com.rexxo.otp.config.OtpConfig;
import com.rexxo.otp.service.EmailProvider;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final OtpConfig otpConfig;
    private final JavaMailSender mailSender;

    @Autowired
    public SmtpEmailProvider(OtpConfig otpConfig, @Autowired(required = false) JavaMailSender mailSender) {
        this.otpConfig = otpConfig;
        this.mailSender = mailSender;
    }

    @Override
    public boolean isConfigured() {
        return mailSender != null
            && otpConfig.getMailHost() != null
            && !otpConfig.getMailHost().isBlank();
    }

    @Override
    public void sendEmail(String to, String subject, String bodyText, String htmlText) {
        if (!isConfigured()) {
            if (otpConfig.isDevMode()) {
                log.info("[DEV MODE] Email to: {} | Subject: {} (Real SMTP not configured)", to, subject);
                return;
            }
            log.warn("SMTP email provider is not configured. Missing MAIL_HOST, MAIL_USERNAME, or MAIL_PASSWORD. Set them in backend/.env.");
            throw new IllegalStateException("Email delivery service is currently not configured on this server.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(otpConfig.getMailFrom(), "REXXZO");
            helper.setTo(to);
            helper.setSubject(subject);

            if (htmlText != null && !htmlText.isBlank()) {
                helper.setText(bodyText, htmlText);
            } else {
                helper.setText(bodyText, false);
            }

            mailSender.send(message);
            log.info("Email successfully dispatched to {}", to);
        } catch (MailAuthenticationException e) {
            log.error("SMTP authentication failed when sending email to {}: {}. Verify MAIL_USERNAME and MAIL_PASSWORD in backend/.env", to, e.getMessage());
            throw new RuntimeException("Email delivery authentication failed with the configured SMTP server.", e);
        } catch (MailSendException e) {
            log.error("SMTP mail send failed for recipient {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to deliver email to recipient. Please verify the email address.", e);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again later.", e);
        }
    }
}
