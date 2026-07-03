package com.app.security.service;

import com.app.security.dto.Auth.AuthLoginResponse;
import com.app.security.dto.Auth.AuthRefreshTokenResponse;
import com.app.security.dto.Auth.AuthRegisterResponse;
import com.app.security.dto.Auth.AuthLoginRequest;
import com.app.security.dto.Auth.AuthRegisterRequest;

public interface AuthService {

    AuthRegisterResponse register(AuthRegisterRequest registerRequest);

    AuthLoginResponse login(AuthLoginRequest loginRequest);

    void logout();

    AuthRegisterResponse registerAdmin(AuthRegisterRequest registerRequest);

    AuthRefreshTokenResponse refreshToken(String authorizationHeader);
}
