package com.rexxo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ShiprocketConfig {

    @Value("${shiprocket.email:}")
    private String email;

    @Value("${shiprocket.password:}")
    private String password;

    @Value("${shiprocket.base-url:https://apiv2.shiprocket.in}")
    private String baseUrl;

    @Value("${shiprocket.pickup-pincode:201310}")
    private String pickupPincode;

    @Value("${shiprocket.pickup-location:Primary}")
    private String pickupLocation;

    /**
     * Sanitized email without accidental leading/trailing whitespace.
     */
    public String getEmail() {
        return email != null ? email.trim() : "";
    }

    /**
     * Sanitized password stripped of accidental surrounding quotes or trailing whitespace.
     */
    public String getPassword() {
        if (password == null) return "";
        String p = password.trim();
        if ((p.startsWith("\"") && p.endsWith("\"")) || (p.startsWith("'") && p.endsWith("'"))) {
            if (p.length() >= 2) {
                p = p.substring(1, p.length() - 1).trim();
            }
        }
        return p;
    }

    public String getBaseUrl() {
        return (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : "https://apiv2.shiprocket.in";
    }

    public String getPickupPincode() {
        return (pickupPincode != null && !pickupPincode.isBlank()) ? pickupPincode.trim() : "201310";
    }

    public String getPickupLocation() {
        return (pickupLocation != null && !pickupLocation.isBlank()) ? pickupLocation.trim() : "Primary";
    }

    /**
     * Returns true only when both email and password are configured.
     * Prevents startup failures when credentials are not yet set.
     */
    public boolean isConfigured() {
        String em = getEmail();
        String pw = getPassword();
        return !em.isBlank() && !pw.isBlank()
            && !em.contains("placeholder") && !pw.contains("placeholder");
    }
}
