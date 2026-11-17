package br.com.sellerintelligence.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${security.rate-limit.requests-per-minute:300}")
    private int maxRequestsPerMinute;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public void resetCounts() {
        requestCounts.clear();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!enabled || path.startsWith("/actuator/health") || path.startsWith("/actuator/info")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        long currentMinute = Instant.now().getEpochSecond() / 60;

        WindowCounter counter = requestCounts.compute(clientKey, (key, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new WindowCounter(currentMinute, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentCount = counter.count.get();
        int remaining = Math.max(0, maxRequestsPerMinute - currentCount);

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (currentCount > maxRequestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", "60");

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Rate limit exceeded (" + maxRequestsPerMinute + " req/min)."
            );
            problem.setTitle("Too Many Requests");
            problem.setType(URI.create("https://sellerintelligence.com.br/errors/rate-limit-exceeded"));
            problem.setProperty("code", "RATE_LIMIT_EXCEEDED");
            problem.setProperty("timestamp", Instant.now());

            response.getWriter().write(objectMapper.writeValueAsString(problem));
            return;
        }

        // Periodic map cleanup when size exceeds threshold
        if (requestCounts.size() > 5000) {
            requestCounts.entrySet().removeIf(e -> e.getValue().minute < currentMinute);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null && !tenantId.isBlank()) {
            return "tenant:" + tenantId;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return "ip:" + (ip != null ? ip : "unknown");
    }

    private static class WindowCounter {
        final long minute;
        final AtomicInteger count;

        WindowCounter(long minute, AtomicInteger count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
