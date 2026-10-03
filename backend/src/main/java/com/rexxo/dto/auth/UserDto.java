package com.rexxo.dto.auth;

import java.time.LocalDateTime;

public record UserDto(
    Long id,
    String name,
    String email,
    String phone,
    String role,
    Boolean emailVerified,
    Boolean phoneVerified,
    LocalDateTime createdAt
) {
    public UserDto(Long id, String name, String email, String phone, String role, LocalDateTime createdAt) {
        this(id, name, email, phone, role, false, false, createdAt);
    }
}
