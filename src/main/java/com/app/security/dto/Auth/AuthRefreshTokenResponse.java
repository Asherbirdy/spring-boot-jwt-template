package com.app.security.dto.Auth;

public class AuthRefreshTokenResponse {

    private final String msg;
    private final JwtAccessToken jwtAccessToken;

    public AuthRefreshTokenResponse(String msg, String accessTokenJWT) {
        this.msg = msg;
        this.jwtAccessToken = new JwtAccessToken(accessTokenJWT);
    }

    public String getMsg() {
        return msg;
    }

    public JwtAccessToken getJwtAccessToken() {
        return jwtAccessToken;
    }

    public static class JwtAccessToken {
        private final String accessTokenJWT;

        public JwtAccessToken(String accessTokenJWT) {
            this.accessTokenJWT = accessTokenJWT;
        }

        public String getAccessTokenJWT() {
            return accessTokenJWT;
        }
    }
}
