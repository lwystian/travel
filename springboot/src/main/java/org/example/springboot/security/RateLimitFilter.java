package org.example.springboot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.util.RedisUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int SENSITIVE_IP_LIMIT_PER_MINUTE = 10;
    private static final int PAYMENT_IP_LIMIT_PER_MINUTE = 30;

    private final RedisUtil redisUtil;

    public RateLimitFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        int limit = limitFor(path);
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate:ip:" + clientIp(request) + ":" + request.getMethod() + ":" + path;
        long count = increment(key);
        if (count > limit) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"429\",\"msg\":\"请求过于频繁，请稍后再试\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int limitFor(String path) {
        if (path.contains("/login")
                || path.contains("/register")
                || path.contains("/sms")
                || path.contains("/email")
                || path.contains("/forget")) {
            return SENSITIVE_IP_LIMIT_PER_MINUTE;
        }
        if (path.contains("/tour-order-pay/pay") || path.contains("/tour-order-pay/notify")) {
            return PAYMENT_IP_LIMIT_PER_MINUTE;
        }
        return 0;
    }

    private long increment(String key) {
        String safeKey = Objects.requireNonNull(key, "rate limit key must not be null");
        long count = redisUtil.incr(safeKey, 1);
        if (count == 1) {
            redisUtil.expire(safeKey, 60);
        }
        return count;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            String firstIp = ip.split(",")[0].trim();
            return Objects.requireNonNull(firstIp, "client ip must not be null");
        }
        return Objects.requireNonNullElse(request.getRemoteAddr(), "unknown");
    }
}
