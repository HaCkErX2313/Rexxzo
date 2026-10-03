package com.rexxo.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads a backend/.env file into the Spring Environment at startup.
 * <p>
 * This runs BEFORE Spring beans are initialized, so properties are
 * available for @Value injection and application.properties placeholders.
 * <p>
 * Register in META-INF/spring.factories or via SpringApplication.addInitializers().
 * <p>
 * SECURITY: .env file must be in .gitignore and never committed.
 * .env.example with empty placeholders is the committed reference.
 */
@Slf4j
public class EnvLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        try {
            java.io.File envInBackend = new java.io.File("backend", ".env");
            java.io.File envInCwd = new java.io.File(".env");
            java.io.File envInParent = new java.io.File("..", ".env");

            io.github.cdimascio.dotenv.DotenvBuilder builder = Dotenv.configure().ignoreIfMissing();

            if (envInBackend.exists()) {
                builder.directory("backend");
            } else if (envInCwd.exists()) {
                builder.directory(".");
            } else if (envInParent.exists()) {
                builder.directory("..");
            }

            Dotenv dotenv = builder.load();

            Map<String, Object> envVars = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                // Only set if not already present in system environment
                if (System.getenv(entry.getKey()) == null) {
                    envVars.put(entry.getKey(), entry.getValue());
                }
            });

            if (!envVars.isEmpty()) {
                context.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource("dotenv", envVars));
                log.info("Loaded {} variable(s) from .env file", envVars.size());
            }

            // Automatic Railway / Cloud DATABASE_URL translation to standard Spring Boot JDBC
            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl != null && !databaseUrl.isBlank() && System.getenv("SPRING_DATASOURCE_URL") == null) {
                try {
                    if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                        java.net.URI dbUri = new java.net.URI(databaseUrl);
                        String userInfo = dbUri.getUserInfo();
                        String username = null;
                        String password = null;
                        if (userInfo != null && userInfo.contains(":")) {
                            String[] parts = userInfo.split(":", 2);
                            username = parts[0];
                            password = parts[1];
                        }
                        int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                        String host = dbUri.getHost();
                        String path = dbUri.getPath();
                        String dbName = (path != null && path.length() > 1) ? path.substring(1) : "rexxo";
                        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

                        Map<String, Object> dbProps = new HashMap<>();
                        dbProps.put("spring.datasource.url", jdbcUrl);
                        if (username != null && System.getenv("SPRING_DATASOURCE_USERNAME") == null && System.getenv("DB_USERNAME") == null) {
                            dbProps.put("spring.datasource.username", username);
                        }
                        if (password != null && System.getenv("SPRING_DATASOURCE_PASSWORD") == null && System.getenv("DB_PASSWORD") == null) {
                            dbProps.put("spring.datasource.password", password);
                        }
                        context.getEnvironment().getPropertySources()
                            .addFirst(new MapPropertySource("railwayDatabaseUrl", dbProps));
                        log.info("Configured JDBC datasource from DATABASE_URL for host: {}", host);
                    }
                } catch (Exception e) {
                    log.warn("Could not automatically parse DATABASE_URL: {}", e.getMessage());
                }
            }

        } catch (DotenvException e) {
            log.debug(".env file not found or invalid — using system environment variables only");
        }
    }
}
