package com.rexxo.dto.auth;

public record RegisterRequest(
    String name,
    String fullName,
    String email,
    String password,
    String phone
) {
    public String resolveName() {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
        if (name != null && !name.trim().isEmpty()) return name.trim();
        return null;
    }
}
