package com.app.security.security;

import com.app.security.dao.MemberDao;
import com.app.security.dao.TokenDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class MySecurityConfig {

    private final JwtUtil jwtUtil;

    private final TokenDao tokenDao;

    private final MemberDao memberDao;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    public MySecurityConfig(JwtUtil jwtUtil, TokenDao tokenDao, MemberDao memberDao) {
        this.jwtUtil = jwtUtil;
        this.tokenDao = tokenDao;
        this.memberDao = memberDao;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                // JWT 為 stateless，不需要 Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // JWT 驗證不需要 CSRF
                .csrf(csrf -> csrf.disable())

                // 設定 CORS 跨域
                .cors(cors -> cors
                        .configurationSource(createCorsConfig())
                )

                // 限流 Filter (只攔 /auth/**，依 IP 5 req/min)
                .addFilterBefore(
                        new RateLimitFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )

                // 添加 JWT Filter
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtUtil, tokenDao, memberDao, cookieSecure),
                        UsernamePasswordAuthenticationFilter.class
                )

                // 設定 api 的權限控制 (role 欄位值: "user", "vip", "movie_manager", "admin")
                // 未認證時回傳 JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            new ObjectMapper().writeValue(response.getOutputStream(),
                                    Map.of("status", 401, "error", "UNAUTHORIZED","requestUrl",request.getRequestURI()));
                        })
                )

                .authorizeHttpRequests(request -> request
                        // 註冊和登入不需要認證
                        .requestMatchers(
                                "/auth/**",
                                "/dev/test",
                                "/error" // 顯示特定Error需
                        ).permitAll()

                        // 登出和查詢目前使用者需要認證
                        .requestMatchers(
                                "/member/**"
                        ).authenticated()

                        .anyRequest().denyAll()
                )

                .build();
    }

    private CorsConfigurationSource createCorsConfig() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
