package com.rexxo;

import com.rexxo.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RexxoApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RexxoApplication.class);
        app.addInitializers(new EnvLoader());
        app.run(args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner dropOutdatedConstraints(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check");
                jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check");
            } catch (Exception ignored) {}
        };
    }
}
