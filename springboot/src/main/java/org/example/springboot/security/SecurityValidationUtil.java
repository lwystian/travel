package org.example.springboot.security;

public final class SecurityValidationUtil {
    private SecurityValidationUtil() {
    }

    public static int clampLimit(Integer value, int defaultValue, int maxValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return Math.min(value, maxValue);
    }

    public static int clampPage(Integer value) {
        if (value == null || value <= 0) {
            return 1;
        }
        return value;
    }
}
