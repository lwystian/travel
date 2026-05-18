package org.example.springboot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LogSanitizer {
    private static final int DEFAULT_MAX_LENGTH = 1000;
    private static final String MASK = "***";

    private LogSanitizer() {
    }

    public static String truncate(String value) {
        return truncate(value, DEFAULT_MAX_LENGTH);
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    public static String sanitizeThrowable(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return truncate(throwable.getClass().getSimpleName() + ": " + throwable.getMessage(), 500);
    }

    public static String serializeArgs(ObjectMapper objectMapper, Object[] args, String[] parameterNames, int maxLength) {
        if (args == null || args.length == 0) {
            return "";
        }

        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            params.put(name, sanitizeValue(name, args[i]));
        }

        try {
            return truncate(objectMapper.writeValueAsString(params), maxLength);
        } catch (Exception e) {
            return truncate(params.toString(), maxLength);
        }
    }

    private static Object sanitizeValue(String name, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveName(name)) {
            return MASK;
        }
        if (value instanceof MultipartFile file) {
            return Map.of("filename", file.getOriginalFilename(), "size", file.getSize());
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse) {
            return value.getClass().getSimpleName();
        }
        if (value instanceof Collection<?> collection) {
            return "Collection(size=" + collection.size() + ")";
        }
        if (isSimpleValue(value)) {
            return truncate(String.valueOf(value), 300);
        }
        return value;
    }

    private static boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    public static boolean isSensitiveName(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("pwd")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("key")
                || normalized.contains("authorization");
    }
}
