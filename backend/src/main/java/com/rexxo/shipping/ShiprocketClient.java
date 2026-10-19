package com.rexxo.shipping;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Interface contract for communicating with Shiprocket API.
 * Enables clean mocking in tests and abstraction over low-level HTTP transport.
 */
public interface ShiprocketClient {

    /** Returns valid bearer token, re-authenticating if necessary. */
    String getBearerToken();

    /** Invalidates cached token to trigger re-authentication. */
    void invalidateToken();

    /** Authenticated GET request. */
    JsonNode get(String path, Map<String, String> query);

    /** Authenticated POST request. */
    JsonNode post(String path, Object payload);

    /** Last sanitized authentication error message. */
    String getLastAuthError();
}
