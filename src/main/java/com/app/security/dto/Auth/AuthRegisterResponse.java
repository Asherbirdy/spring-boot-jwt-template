package com.app.security.dto.Auth;

public class AuthRegisterResponse {
    private final String name;
    private final String memberId;
    private final String role;

    public AuthRegisterResponse(String name, String memberId, String role) {
        this.name = name;
        this.memberId = memberId;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getRole() {
        return role;
    }
}
