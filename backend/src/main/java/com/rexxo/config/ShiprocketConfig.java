package com.rexxo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ShiprocketConfig {

    @Value("${shiprocket.email}")
    private String email;

    @Value("${shiprocket.password}")
    private String password;

    @Value("${shiprocket.base-url}")
    private String baseUrl;

    @Value("${shiprocket.pickup-pincode}")
    private String pickupPincode;

    /**
     * Returns true only when both email and password are configured.
     * Prevents startup failures when credentials are not yet set.
     */
    public boolean isConfigured() {
        return email != null && !email.isBlank()
            && password != null && !password.isBlank();
    }
}
