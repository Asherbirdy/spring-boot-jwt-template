package com.app.security.service.impl;

import com.app.security.support.AuthTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthServiceImplTest extends AuthTestSupport {

    /**
     * 驗證：正確帳密打 /auth/login，應回 200，並在回應寫入 accessToken / refreshToken cookie
     * （token 只走 cookie，response body 不再含 tokenPair）。
     */
    @Test
    @DisplayName("login: 正確帳密寫入 accessToken / refreshToken cookie")
    public void login_correctCredentials_returnsToken() throws Exception {
        Map<String, String> body = Map.of(
                "email", "user@gmail.com",
                "password", "password"
        );

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", randomIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenPair").doesNotExist())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));
    }

    /**
     * 驗證：密碼錯誤時 /auth/login 應回 400 WRONG_EMAIL_OR_PASSWORD。
     */
    @Test
    @DisplayName("login: 密碼錯誤 → 400 WRONG_EMAIL_OR_PASSWORD")
    public void login_wrongPassword_returnsBadRequest() throws Exception {
        Map<String, String> body = Map.of(
                "email", "admin@gmail.com",
                "password", "wrong-password"
        );

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", randomIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andReturn();

        org.junit.jupiter.api.Assertions.assertTrue(
                result.getResponse().getContentAsString().contains("WRONG_EMAIL_OR_PASSWORD"),
                "expected response to mention WRONG_EMAIL_OR_PASSWORD");
    }

    /**
     * 驗證：登入後帶 accessToken cookie 打 /member/showMe，應回 200 並回傳會員 email。
     */
    @Test
    @DisplayName("showMe: 帶有效 accessToken 回傳會員資料")
    public void showMe_withValidToken_returnsMemberInfo() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/member/showMe")
                        .cookie(userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"));
    }
}
