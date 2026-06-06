package org.example.springboot.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestMetadataUtil {
    private RequestMetadataUtil() {
    }

    public static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = firstNonBlank(
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getHeader("Proxy-Client-IP"),
                request.getHeader("WL-Proxy-Client-IP"),
                request.getHeader("HTTP_CLIENT_IP"),
                request.getHeader("HTTP_X_FORWARDED_FOR"),
                request.getRemoteAddr()
        );
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return truncate(ip, 100);
    }

    public static Integer clientPort(HttpServletRequest request) {
        if (request == null) {
            return 0;
        }
        return request.getRemotePort();
    }

    public static String userAgent(HttpServletRequest request) {
        return firstHeader(request, 500, "X-Client-User-Agent", "User-Agent");
    }

    public static String deviceId(HttpServletRequest request) {
        return firstHeader(request, 128, "X-Device-Id", "X-Client-Device-Id");
    }

    public static String deviceFingerprint(HttpServletRequest request) {
        return firstHeader(request, 255, "X-Device-Fingerprint", "X-Client-Fingerprint");
    }

    public static String clientHardware(HttpServletRequest request) {
        return firstHeader(request, 500, "X-Client-Hardware", "X-Hardware-Fingerprint", "Sec-CH-UA-Platform", "Sec-CH-UA");
    }

    public static String macAddress(HttpServletRequest request) {
        return firstHeader(request, 64, "X-Mac-Address", "X-Client-Mac");
    }

    private static String firstHeader(HttpServletRequest request, int maxLength, String... names) {
        if (request == null) {
            return "";
        }
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return truncate(value.trim(), maxLength);
            }
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value.trim();
            }
        }
        return "unknown";
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
