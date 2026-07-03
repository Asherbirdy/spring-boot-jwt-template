package com.app.security.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AuthTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected Cookie adminAccessToken;
    protected Cookie userAccessToken;

    @BeforeAll
    void setUp() throws Exception {
        adminAccessToken = loginAndGetAccessToken("admin@gmail.com", "password");
        userAccessToken = loginAndGetAccessToken("user@gmail.com", "password");
    }

    protected Cookie loginAndGetAccessToken(String email, String password) throws Exception {
        Map<String, String> body = Map.of(
                "email", email,
                "password", password
        );

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", randomIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("accessToken");
    }

    private static final java.util.concurrent.atomic.AtomicInteger ipCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    protected static String randomIp() {
        int n = ipCounter.incrementAndGet();
        // 10.<a>.<b>.<c> — 不會與真實 client IP 衝突的私有網段
        return "10." + ((n >> 16) & 0xff) + "." + ((n >> 8) & 0xff) + "." + (n & 0xff);
    }
}
