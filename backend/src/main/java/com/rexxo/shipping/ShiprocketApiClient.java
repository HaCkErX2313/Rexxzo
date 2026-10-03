package com.rexxo.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.config.ShiprocketConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Low-level HTTP client for the Shiprocket API.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Authenticate using email/password and cache the bearer token.</li>
 *   <li>Automatically re-authenticate when the token is expired or rejected.</li>
 *   <li>Provide typed GET/POST helpers used by ShiprocketService.</li>
 *   <li>NEVER log passwords, tokens, or Authorization headers.</li>
 * </ul>
 * <p>
 * Token validity: Shiprocket tokens are valid for 240 hours (10 days).
 * We refresh 1 hour before expiry to be safe.
 */
@Component
@Slf4j
public class ShiprocketApiClient {

    // 240 hours minus 1 hour safety buffer, in seconds
    private static final long TOKEN_VALIDITY_SECONDS = (240 - 1) * 3600L;

    private final ShiprocketConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /** Cached bearer token (without "Bearer " prefix) */
    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    /** When the cached token was obtained (epoch seconds) */
    private volatile long tokenObtainedAt = 0L;

    public ShiprocketApiClient(ShiprocketConfig config, WebClient.Builder webClientBuilder,
                               ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
            .baseUrl(config.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    // ─── Authentication ──────────────────────────────────────────────────────

    /**
     * Returns a valid bearer token, re-authenticating if necessary.
     * Never logs or returns the password or token to callers.
     */
    public String getBearerToken() {
        if (isTokenValid()) {
            return cachedToken.get();
        }
        return authenticate();
    }

    private boolean isTokenValid() {
        String token = cachedToken.get();
        if (token == null || token.isBlank()) return false;
        long elapsed = Instant.now().getEpochSecond() - tokenObtainedAt;
        return elapsed < TOKEN_VALIDITY_SECONDS;
    }

    /**
     * Authenticates with Shiprocket and caches the token.
     * Logs only success/failure — never the actual token or password.
     */
    private synchronized String authenticate() {
        // Double-check after acquiring lock
        if (isTokenValid()) return cachedToken.get();

        if (!config.isConfigured()) {
            throw new ShiprocketException("Shiprocket credentials are not configured. " +
                "Set SHIPROCKET_EMAIL and SHIPROCKET_PASSWORD environment variables.");
        }

        log.info("Authenticating with Shiprocket API...");

        try {
            Map<String, String> body = Map.of(
                "email", config.getEmail(),
                "password", config.getPassword()
            );

            String responseBody = webClient.post()
                .uri("/v1/external/auth/login")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                    response.bodyToMono(String.class)
                        .map(err -> new ShiprocketException("Authentication failed with status "
                            + response.statusCode().value() + ". Check credentials.")))
                .bodyToMono(String.class)
                .block();

            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode tokenNode = json.get("token");
            if (tokenNode == null || tokenNode.isNull() || tokenNode.asText().isBlank()) {
                throw new ShiprocketException("Shiprocket authentication response missing token field.");
            }

            String token = tokenNode.asText();
            cachedToken.set(token);
            tokenObtainedAt = Instant.now().getEpochSecond();

            // ⚠ IMPORTANT: We NEVER log the token value
            log.info("SHIPROCKET AUTHENTICATION: SUCCESS");
            return token;

        } catch (ShiprocketException e) {
            log.error("SHIPROCKET AUTHENTICATION: FAILED — {}", e.getMessage());
            throw e;
        } catch (WebClientResponseException e) {
            log.error("SHIPROCKET AUTHENTICATION: FAILED — HTTP {} {}",
                e.getStatusCode().value(), e.getStatusText());
            throw new ShiprocketException("Shiprocket authentication HTTP error: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("SHIPROCKET AUTHENTICATION: FAILED — Unexpected error: {}", e.getMessage());
            throw new ShiprocketException("Shiprocket authentication failed: " + e.getMessage());
        }
    }

    /** Invalidates the cached token so next call re-authenticates. */
    public void invalidateToken() {
        cachedToken.set(null);
        tokenObtainedAt = 0L;
        log.info("Shiprocket token invalidated — will re-authenticate on next call");
    }

    // ─── HTTP Helpers ────────────────────────────────────────────────────────

    /**
     * Performs an authenticated GET request.
     *
     * @param path  API path, e.g. "/v1/external/courier/serviceability/"
     * @param query query parameters map
     * @return parsed JsonNode response
     */
    public JsonNode get(String path, Map<String, String> query) {
        return executeWithRetry(() -> {
            WebClient.RequestHeadersSpec<?> spec = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (query != null) {
                        query.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getBearerToken());

            return spec.retrieve()
                .onStatus(status -> status.value() == 401, response ->
                    response.bodyToMono(String.class)
                        .map(body -> new ShiprocketTokenExpiredException("Token expired or invalid")))
                .onStatus(HttpStatusCode::isError, response ->
                    response.bodyToMono(String.class)
                        .map(body -> new ShiprocketException("GET " + path + " failed: " + body)))
                .bodyToMono(String.class)
                .block();
        });
    }

    /**
     * Performs an authenticated POST request.
     *
     * @param path    API path
     * @param payload request body object (will be serialized to JSON)
     * @return parsed JsonNode response
     */
    public JsonNode post(String path, Object payload) {
        return executeWithRetry(() -> {
            return webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getBearerToken())
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.value() == 401, response ->
                    response.bodyToMono(String.class)
                        .map(body -> new ShiprocketTokenExpiredException("Token expired or invalid")))
                .onStatus(HttpStatusCode::isError, response ->
                    response.bodyToMono(String.class)
                        .map(body -> new ShiprocketException("POST " + path + " failed: " + body)))
                .bodyToMono(String.class)
                .block();
        });
    }

    // ─── Retry Logic ─────────────────────────────────────────────────────────

    /**
     * Executes an API call, and on 401/token-expired, invalidates the token
     * and retries exactly once.
     */
    private JsonNode executeWithRetry(ApiCall call) {
        try {
            String raw = call.execute();
            return parseJson(raw);
        } catch (ShiprocketTokenExpiredException e) {
            log.info("Shiprocket token expired — re-authenticating and retrying once");
            invalidateToken();
            try {
                String raw = call.execute();
                return parseJson(raw);
            } catch (Exception retryEx) {
                log.error("Shiprocket request failed after token refresh: {}", retryEx.getMessage());
                throw new ShiprocketException("Request failed after token refresh: " + retryEx.getMessage());
            }
        } catch (ShiprocketException e) {
            throw e;
        } catch (Exception e) {
            log.error("Shiprocket API call failed: {}", e.getMessage());
            throw new ShiprocketException("Shiprocket API call failed: " + e.getMessage());
        }
    }

    private JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ShiprocketException("Failed to parse Shiprocket response: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ApiCall {
        String execute();
    }

    // ─── Exception Types ─────────────────────────────────────────────────────

    public static class ShiprocketException extends RuntimeException {
        public ShiprocketException(String message) { super(message); }
    }

    public static class ShiprocketTokenExpiredException extends ShiprocketException {
        public ShiprocketTokenExpiredException(String message) { super(message); }
    }
}
