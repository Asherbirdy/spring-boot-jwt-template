package com.app.security.service.impl;

import com.app.security.dao.MemberDao;
import com.app.security.dao.TokenDao;
import com.app.security.dto.Auth.*;
import com.app.security.model.Member;
import com.app.security.model.Token;
import com.app.security.security.JwtUtil;
import com.app.security.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Component
public class AuthServiceImpl implements AuthService {

    private final MemberDao memberDao;

    private final TokenDao tokenDao;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    public AuthServiceImpl(MemberDao memberDao, TokenDao tokenDao,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.memberDao = memberDao;
        this.tokenDao = tokenDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthRegisterResponse register(AuthRegisterRequest registerRequest) {
        return createMemberWithRole(registerRequest, "user");
    }

    @Override
    public AuthRegisterResponse registerAdmin(AuthRegisterRequest registerRequest) {
        // 檢查是否已有 admin
        List<Member> existingAdmins = memberDao.getRoleMembers("admin");
        if (!existingAdmins.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ADMIN_ALREADY_EXISTS");
        }
        return createMemberWithRole(registerRequest, "admin");
    }

    private AuthRegisterResponse createMemberWithRole(AuthRegisterRequest registerRequest, String role) {
        String name = registerRequest.getName();
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();

        // 檢查 email 是否已被註冊
        Member existingMember = memberDao.getMemberByEmail(email);
        if (existingMember != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMAIL_ALREADY_USED");
        }

        // hash 密碼並建立會員
        String hashedPassword = passwordEncoder.encode(password);
        Member member = new Member();
        member.setName(name);
        member.setEmail(email);
        member.setPassword(hashedPassword);
        member.setRole(role);

        String memberId = memberDao.createMember(member);

        // 建立 refresh token 並設定 Cookie
        String refreshTokenStr = createRefreshToken(memberId);
        attachCookieToResponse(memberId, name, email, role, refreshTokenStr);

        return new AuthRegisterResponse(name, memberId, role);
    }

    @Override
    public AuthLoginResponse login(AuthLoginRequest loginRequest) {
        Member member = authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        return issueTokens(member);
    }

    private Member authenticate(String email, String password) {
        Member member = memberDao.getMemberByEmail(email);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "WRONG_EMAIL_OR_PASSWORD");
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "WRONG_EMAIL_OR_PASSWORD");
        }
        return member;
    }

    private AuthLoginResponse issueTokens(Member member) {
        String memberId = member.getMemberId();
        String refreshTokenStr;
        Token existingToken = tokenDao.getValidTokenByMemberId(memberId);
        if (existingToken != null) {
            refreshTokenStr = existingToken.getRefreshToken();
        } else {
            refreshTokenStr = createRefreshToken(memberId);
        }

        attachCookieToResponse(memberId, member.getName(), member.getEmail(), member.getRole(), refreshTokenStr);
        return new AuthLoginResponse(member.getName(), memberId, member.getRole());
    }

    @Override
    public void logout() {
        String memberId = getCurrentMemberId();
        HttpServletResponse response = getCurrentResponse();

        // 刪除該使用者所有 token
        tokenDao.deleteTokensByMemberId(memberId);

        // 清除 Cookie（maxAge=0 讓瀏覽器立即刪除）
        response.addHeader(HttpHeaders.SET_COOKIE, buildTokenCookie("accessToken", "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildTokenCookie("refreshToken", "", 0).toString());
    }

    @Override
    public AuthRefreshTokenResponse refreshToken(String authorizationHeader) {
        // 從 Authorization header 取出 refreshToken（Bearer scheme）
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_INVALID");
        }
        String refreshTokenJwt = authorizationHeader.substring("Bearer ".length()).trim();
        if (refreshTokenJwt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_INVALID");
        }

        // 解析 refreshToken JWT
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshTokenJwt);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_INVALID");
        }

        String memberId = claims.get("memberId", String.class);
        String refreshTokenStr = claims.get("refreshToken", String.class);
        if (memberId == null || refreshTokenStr == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_INVALID");
        }

        // 確認 token 仍存在 DB 且為 valid
        Token existingToken = tokenDao.getValidTokenByMemberId(memberId);
        if (existingToken == null || !refreshTokenStr.equals(existingToken.getRefreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_INVALID");
        }

        // IP 一致性檢查
        HttpServletRequest request = getCurrentRequest();
        String currentIp = request.getRemoteAddr();
        if (existingToken.getIp() != null && !existingToken.getIp().equals(currentIp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "IP_MISMATCH");
        }

        // 取最新會員資料，重新產生 accessToken
        Member member = memberDao.getMemberById(memberId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_INVALID");
        }
        String newAccessToken = jwtUtil.createAccessToken(
                memberId, member.getName(), member.getEmail(), member.getRole());

        return new AuthRefreshTokenResponse("TOKEN_REFRESHED", newAccessToken);
    }

    private ServletRequestAttributes getRequestAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }

    private HttpServletRequest getCurrentRequest() {
        return getRequestAttributes().getRequest();
    }

    private HttpServletResponse getCurrentResponse() {
        return getRequestAttributes().getResponse();
    }

    private String getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (String) authentication.getCredentials();
    }

    private String createRefreshToken(String memberId) {
        HttpServletRequest request = getCurrentRequest();
        String refreshTokenStr = UUID.randomUUID().toString();
        Token token = new Token();
        token.setRefreshToken(refreshTokenStr);
        token.setIp(request.getRemoteAddr());
        token.setUserAgent(request.getHeader("User-Agent"));
        token.setIsValid(true);
        token.setMemberId(memberId);
        tokenDao.createToken(token);
        return refreshTokenStr;
    }

    private void attachCookieToResponse(String memberId, String name, String email, String role, String refreshTokenStr) {
        HttpServletResponse response = getCurrentResponse();
        String accessTokenJwt = jwtUtil.createAccessToken(memberId, name, email, role);
        String refreshTokenJwt = jwtUtil.createRefreshToken(memberId, email, refreshTokenStr);

        ResponseCookie accessCookie = buildTokenCookie(
                "accessToken", accessTokenJwt, jwtUtil.getAccessTokenExpirationMs() / 1000);
        ResponseCookie refreshCookie = buildTokenCookie(
                "refreshToken", refreshTokenJwt, jwtUtil.getRefreshTokenExpirationMs() / 1000);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    /**
     * 統一建立 token cookie：HttpOnly + Secure + SameSite=None，供跨網域前後端分離使用。
     * maxAgeSeconds 傳 0 代表要求瀏覽器立即刪除該 cookie。
     */
    private ResponseCookie buildTokenCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(maxAgeSeconds)
                .build();
    }

}
