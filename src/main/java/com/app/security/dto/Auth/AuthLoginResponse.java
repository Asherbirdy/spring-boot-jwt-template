package com.app.security.dto.Auth;

public class AuthLoginResponse {
    private final String name;
    private final String memberId;
    private final String role;
    private final AuthTokenPair tokenPair;

    public AuthLoginResponse(String name, String memberId, String role, AuthTokenPair tokenPair) {
        this.name = name;
        this.memberId = memberId;
        this.role = role;
        this.tokenPair = tokenPair;
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

    public AuthTokenPair getTokenPair() {
        return tokenPair;
    }
}
