package com.rexxo.otp.service.impl;

import com.rexxo.otp.config.OtpConfig;
import com.rexxo.otp.service.EmailOtpService;
import com.rexxo.otp.service.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultEmailOtpService implements EmailOtpService {

    private final EmailProvider emailProvider;
    private final OtpConfig otpConfig;

    @Override
    public void sendOtp(String email, String otp) {
        String subject = "Verify your REXXZO account";

        String textBody = String.format(
            "Hello,\n\n" +
            "Your REXXZO verification code is:\n\n" +
            "%s\n\n" +
            "This code expires in %d minutes.\n\n" +
            "If you did not request this code, you can ignore this email.\n\n" +
            "REXXZO\n" +
            "BETTER EVERYDAY",
            otp, otpConfig.getExpirationMinutes()
        );

        String htmlBody = String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset='utf-8'>" +
            "<style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f9f8f6; color: #080808; margin: 0; padding: 40px 20px; }" +
            ".card { max-width: 480px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e1d8; border-radius: 12px; padding: 36px; box-shadow: 0 4px 20px rgba(0,0,0,0.04); }" +
            ".logo { font-size: 20px; font-weight: 900; letter-spacing: 0.25em; text-transform: uppercase; color: #080808; margin-bottom: 24px; text-align: center; }" +
            ".title { font-size: 18px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: #080808; margin-bottom: 12px; }" +
            ".otp-box { background: #080808; color: #C8BCA7; font-size: 32px; font-weight: 800; letter-spacing: 0.35em; text-align: center; padding: 18px; border-radius: 8px; margin: 24px 0; font-family: monospace; }" +
            ".info { font-size: 13px; line-height: 1.6; color: #555555; margin-bottom: 16px; }" +
            ".footer { font-size: 11px; color: #8c7f69; border-top: 1px solid #e5e1d8; padding-top: 18px; margin-top: 24px; text-align: center; text-transform: uppercase; letter-spacing: 0.1em; font-weight: 600; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='card'>" +
            "  <div class='logo'>REXXZO</div>" +
            "  <div class='title'>Verify Your Account</div>" +
            "  <p class='info'>Hello,</p>" +
            "  <p class='info'>Use the verification code below to complete your REXXZO registration. This code expires in <strong>%d minutes</strong>.</p>" +
            "  <div class='otp-box'>%s</div>" +
            "  <p class='info'>If you did not request this verification code, please ignore this email.</p>" +
            "  <div class='footer'>REXXZO &bull; BETTER EVERYDAY</div>" +
            "</div>" +
            "</body>" +
            "</html>",
            otpConfig.getExpirationMinutes(), otp
        );

        emailProvider.sendEmail(email, subject, textBody, htmlBody);
    }
}
