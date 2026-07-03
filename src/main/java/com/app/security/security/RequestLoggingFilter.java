package com.app.security.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * API 請求 log filter：每支 API 進來都會印出 method、URI、IP、payload、耗時等資訊。
 * 繼承 OncePerRequestFilter 確保同一個 request 在 forward/include 時不會被印兩次。
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // payload 最大長度，超過會截斷（避免大檔上傳灌爆 log）
    private static final int MAX_PAYLOAD_LENGTH = 2000;

    // 敏感欄位，log 時會被遮罩成 ***
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "newPassword", "oldPassword", "confirmPassword"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 用 ContentCaching wrapper 包起來，這樣 body 讀過後仍可重複讀取，不影響 controller
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // 不論成功或例外都要印 log，並把快取的 response body 寫回真正的輸出流
            long elapsed = System.currentTimeMillis() - start;
            logRequest(wrappedRequest, wrappedResponse, elapsed);
            wrappedResponse.copyBodyToResponse();
        }
    }

    /** 組合並輸出單行 log */
    private void logRequest(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            long elapsedMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String ip = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        int status = response.getStatus();
        String payload = extractPayload(request.getContentAsByteArray(), request.getCharacterEncoding());

        log.info("[API] {} {}{} | status={} | {}ms | ip={} | ua={} | payload={}",
                method,
                uri,
                query == null ? "" : "?" + query,
                status,
                elapsedMs,
                ip,
                userAgent,
                payload);
    }

    /**
     * 取得真正的 client IP。
     * 若有經過 proxy / load balancer，優先讀 X-Forwarded-For 與 X-Real-IP，
     * 否則回傳 socket 上看到的 remote address。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For 可能是逗號分隔的多個 IP，第一個才是原始 client
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    /** 將 request body 轉成可讀字串，並做敏感欄位遮罩與長度截斷 */
    private String extractPayload(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) {
            return "-";
        }
        String charset = encoding != null ? encoding : StandardCharsets.UTF_8.name();
        String raw;
        try {
            raw = new String(buf, charset);
        } catch (Exception e) {
            // 二進位內容（例如檔案上傳）無法用文字解讀
            return "<unreadable>";
        }
        String masked = maskSensitive(raw);
        if (masked.length() > MAX_PAYLOAD_LENGTH) {
            return masked.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
        }
        return masked;
    }

    /** 嘗試把 raw 當作 JSON 解析並遮罩敏感欄位；若不是 JSON 就原樣返回 */
    private String maskSensitive(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            maskNode(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return raw;
        }
    }

    /** 遞迴走訪 JSON 樹，把 SENSITIVE_FIELDS 命中的欄位值換成 *** */
    private void maskNode(JsonNode node) {
        if (node.isArray()) {
            node.forEach(this::maskNode);
            return;
        }
        if (!(node instanceof ObjectNode obj)) {
            // 葉子節點（字串、數字、boolean、null）不需處理
            return;
        }
        obj.fieldNames().forEachRemaining(name -> {
            if (SENSITIVE_FIELDS.contains(name)) {
                obj.put(name, "***");
                return;
            }
            maskNode(obj.get(name));
        });
    }
}
