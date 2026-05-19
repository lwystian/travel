package org.example.springboot.security;

import java.util.regex.Pattern;

public final class XssSanitizer {
    private static final Pattern SCRIPT_BLOCK = Pattern.compile("(?is)<\\s*(script|iframe|object|embed|link|meta|base|style)[^>]*>.*?<\\s*/\\s*\\1\\s*>");
    private static final Pattern DANGEROUS_TAG = Pattern.compile("(?is)<\\s*/?\\s*(script|iframe|object|embed|link|meta|base|style)[^>]*>");
    private static final Pattern EVENT_HANDLER = Pattern.compile("(?i)\\s+on[a-z0-9_-]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern JS_PROTOCOL = Pattern.compile("(?i)(href|src|xlink:href)\\s*=\\s*(\"|')\\s*(javascript|data:text/html|vbscript):.*?\\2");
    private static final Pattern STYLE_EXPRESSION = Pattern.compile("(?i)\\s+style\\s*=\\s*(\"[^\"]*(expression\\s*\\(|javascript:|vbscript:)[^\"]*\"|'[^']*(expression\\s*\\(|javascript:|vbscript:)[^']*')");

    private XssSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String cleaned = SCRIPT_BLOCK.matcher(value).replaceAll("");
        cleaned = DANGEROUS_TAG.matcher(cleaned).replaceAll("");
        cleaned = EVENT_HANDLER.matcher(cleaned).replaceAll("");
        cleaned = JS_PROTOCOL.matcher(cleaned).replaceAll("$1=\"#\"");
        cleaned = STYLE_EXPRESSION.matcher(cleaned).replaceAll("");
        return cleaned;
    }
}
