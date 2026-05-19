package org.example.springboot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CsrfOriginFilter extends OncePerRequestFilter {
    private final SecurityProperties securityProperties;

    public CsrfOriginFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!securityProperties.isOriginCheckEnabled() || isSafeMethod(request.getMethod()) || isPublicCallback(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        if (isAllowedOrigin(origin) || isAllowedReferer(referer) || isSameOriginRequestWithoutBrowserOrigin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityResponseUtil.writeForbidden(response, "非法跨站请求");
    }

    private boolean isSafeMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }

    private boolean isPublicCallback(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/tour-order-pay/notify") || path.startsWith("/api/payment/");
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        return securityProperties.getAllowedOrigins().stream().anyMatch(allowed -> {
            if (allowed.equalsIgnoreCase(origin)) {
                return true;
            }
            if (allowed.contains("*")) {
                String regex = allowed
                        .replace(".", "\\.")
                        .replace("*", ".*");
                return origin.matches(regex);
            }
            return false;
        });
    }

    private boolean isAllowedReferer(String referer) {
        if (referer == null || referer.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(referer);
            String origin = uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getAuthority();
            return isAllowedOrigin(origin);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isSameOriginRequestWithoutBrowserOrigin(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }
}
