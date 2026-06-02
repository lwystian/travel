package org.example.springboot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SqlInjectionFilter extends OncePerRequestFilter {
    private static final Pattern SQLI_PATTERN = Pattern.compile(
            "(?i)(?:--|/\\*|\\*/|;\\s*(?:select|insert|update|delete|drop|alter|truncate|create)|" +
                    "\\b(?:union\\s+select|sleep\\s*\\(|benchmark\\s*\\(|load_file\\s*\\(|outfile\\b|information_schema\\b)\\b|" +
                    "\\b(?:or|and)\\b\\s+\\d+\\s*=\\s*\\d+)"
    );

    private final SecurityProperties securityProperties;

    public SqlInjectionFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!securityProperties.isSqlInjectionCheckEnabled() || !request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (containsSqlInjection(request.getQueryString()) || containsSqlInjectionInParameters(request)) {
            SecurityResponseUtil.writeForbidden(response, "请求包含非法SQL特征");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean containsSqlInjectionInParameters(HttpServletRequest request) {
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (containsSqlInjection(name)) {
                return true;
            }
            for (String value : request.getParameterValues(name)) {
                if (containsSqlInjection(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsSqlInjection(String value) {
        return value != null && SQLI_PATTERN.matcher(value).find();
    }
}
