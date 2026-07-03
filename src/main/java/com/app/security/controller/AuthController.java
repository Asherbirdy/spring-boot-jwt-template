package com.app.security.controller;

import com.app.security.dto.Auth.AuthLoginResponse;
import com.app.security.dto.Auth.AuthRefreshTokenResponse;
import com.app.security.dto.Auth.AuthRegisterResponse;
import com.app.security.dto.Auth.LoginRequest;
import com.app.security.dto.Auth.RegisterRequest;
import com.app.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 這邊放無需驗證的 controller
@RequestMapping("/auth")
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 一般使用者註冊（role = user）。
     * 用於前台會員自助註冊流程。
     */
    @PostMapping("/register")
    public ResponseEntity<AuthRegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthRegisterResponse user = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * 使用 email + 密碼登入，成功後回傳 tokenPair（accessToken / refreshToken），
     * 並將 access / refresh token 寫入 HttpOnly cookie。
     */
    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthLoginResponse user = authService.login(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    /**
     * 建立 admin 帳號（role = admin）。
     * 用於系統初始化或內部建立後台管理員。
     */
    @PostMapping("/register-admin")
    public ResponseEntity<AuthRegisterResponse> registerAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthRegisterResponse admin = authService.registerAdmin(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }

    /**
     * 用 refreshToken 換新的 accessToken。
     * Header: Authorization: Bearer <refreshToken>
     * 用於前端 accessToken 過期 / 不存在時，靜默換 token，免重新登入。
     */
    @GetMapping("/refreshToken")
    public ResponseEntity<AuthRefreshTokenResponse> refreshToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        AuthRefreshTokenResponse response = authService.refreshToken(authorizationHeader);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
