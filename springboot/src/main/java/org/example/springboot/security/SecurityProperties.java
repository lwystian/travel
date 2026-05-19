package org.example.springboot.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            "http://localhost:8081",
            "http://127.0.0.1:8081",
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://192.168.*:*"
    ));
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Content-Type",
            "token",
            "X-Requested-With",
            "X-CSRF-Token"
    ));
    private boolean originCheckEnabled = true;
    private boolean sqlInjectionCheckEnabled = true;
    private boolean xssProtectionEnabled = true;
    private boolean passwordEncryptionEnabled = false;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isOriginCheckEnabled() {
        return originCheckEnabled;
    }

    public void setOriginCheckEnabled(boolean originCheckEnabled) {
        this.originCheckEnabled = originCheckEnabled;
    }

    public boolean isSqlInjectionCheckEnabled() {
        return sqlInjectionCheckEnabled;
    }

    public void setSqlInjectionCheckEnabled(boolean sqlInjectionCheckEnabled) {
        this.sqlInjectionCheckEnabled = sqlInjectionCheckEnabled;
    }

    public boolean isXssProtectionEnabled() {
        return xssProtectionEnabled;
    }

    public void setXssProtectionEnabled(boolean xssProtectionEnabled) {
        this.xssProtectionEnabled = xssProtectionEnabled;
    }

    public boolean isPasswordEncryptionEnabled() {
        return passwordEncryptionEnabled;
    }

    public void setPasswordEncryptionEnabled(boolean passwordEncryptionEnabled) {
        this.passwordEncryptionEnabled = passwordEncryptionEnabled;
    }
}
