package com.app.security.security;

import com.app.security.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * /auth/** 端點的 IP 限流 Filter。
 *
 * <p>演算法：Token Bucket（Bucket4j）。每個 client IP 一個獨立的桶。
 * <ul>
 *   <li>{@link #CAPACITY}：桶子容量 = 每個時窗最多允許的請求數。</li>
 *   <li>{@link #REFILL_PERIOD}：補滿週期。每經過此週期一次性補到滿（refillIntervally）。</li>
 * </ul>
 *
 * <p>桶子過期清理：避免 {@code buckets} map 因攻擊者 / 短期 client 無限增長，
 * 用一條 daemon 排程執行緒定期掃描並丟棄閒置太久的桶。
 * <ul>
 *   <li>{@link #IDLE_EVICT_NANOS}：超過此閒置時間（最後一次存取後）的桶會被丟棄。
 *       設定要大於 {@link #REFILL_PERIOD}，避免把還沒補滿的桶提早刪掉造成限流失效。</li>
 *   <li>{@link #CLEANUP_INTERVAL_SECONDS}：掃描頻率。越短越即時但 CPU 成本越高。</li>
 * </ul>
 *
 * <p>限制：狀態存在單機記憶體，重啟會清空，多台 server 各算各的。
 * 之後要 scale 改成 Bucket4j + Redis 即可共享 quota。
 */
public class RateLimitFilter extends OncePerRequestFilter {

    // 桶子容量：每個時窗最多允許的請求數
    private static final int CAPACITY = 5;

    // 補滿週期：每經過此週期一次性把 token 補到 CAPACITY
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    // 只攔截此前綴的路徑（其他路徑走 shouldNotFilter 跳過）
    private static final String PATH_PREFIX = "/auth/";

    // 桶子閒置超過此時間就會被清理執行緒丟棄；必須大於 REFILL_PERIOD
    private static final long IDLE_EVICT_NANOS = Duration.ofMinutes(10).toNanos();

    // 清理執行緒掃描間隔（秒）
    private static final long CLEANUP_INTERVAL_SECONDS = 60;

    private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter() {
        // daemon 執行緒：JVM 關閉時自動結束，不用顯式 shutdown
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::evictIdleBuckets,
                CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String key = resolveClientIp(request);
        Entry entry = buckets.computeIfAbsent(key, k -> new Entry(newBucket()));
        entry.lastAccessNanos.set(System.nanoTime());
        ConsumptionProbe probe = entry.bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1)));
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiResponse<>("Too many requests, please try again later.", null)
        );
    }

    /**
     * 由排程執行緒定期呼叫：掃描所有桶，丟棄閒置超過 {@link #IDLE_EVICT_NANOS} 的 entry。
     * 用 {@code remove(key, value)} 而非 {@code remove(key)}，避免誤刪剛被其他執行緒
     * 更新 lastAccess 的 entry（race condition 防護）。
     */
    private void evictIdleBuckets() {
        long now = System.nanoTime();
        for (Map.Entry<String, Entry> e : buckets.entrySet()) {
            if (now - e.getValue().lastAccessNanos.get() > IDLE_EVICT_NANOS) {
                buckets.remove(e.getKey(), e.getValue());
            }
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(CAPACITY)
                        .refillIntervally(CAPACITY, REFILL_PERIOD)
                        .build())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Entry {
        final Bucket bucket;
        final AtomicLong lastAccessNanos;

        Entry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessNanos = new AtomicLong(System.nanoTime());
        }
    }
}
