package com.rexxo.dto.auth;

public record ApiResponseMessage(
    boolean success,
    String message
) {
    public static ApiResponseMessage of(boolean success, String message) {
        return new ApiResponseMessage(success, message);
    }
}
