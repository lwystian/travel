package org.example.springboot.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Resource
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/user/login",
                                "/api/auth/geetest/public",
                                "/api/auth/crypto/public-key",
                                "/api/auth/sms/send",
                                "/api/auth/register/phone",
                                "/api/auth/login/password",
                                "/api/auth/login/code",
                                "/api/auth/agreement",
                                "/auth/geetest/public",
                                "/auth/crypto/public-key",
                                "/auth/sms/send",
                                "/auth/register/phone",
                                "/auth/login/password",
                                "/auth/login/code",
                                "/auth/agreement"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://webapi.amap.com https://*.amap.com https://openapi.alipay.com https://mapi.alipay.com https://static.geetest.com https://*.geetest.com; " +
                                        "style-src 'self' 'unsafe-inline' https://webapi.amap.com https://*.amap.com; " +
                                        "img-src 'self' data: blob: https:; " +
                                        "font-src 'self' data:; " +
                                        "connect-src 'self' https://webapi.amap.com https://*.amap.com https://openapi.alipay.com https://mapi.alipay.com https://gcaptcha4.geetest.com https://*.geetest.com; " +
                                        "frame-src 'self' https://*.geetest.com; " +
                                        "frame-ancestors 'self'; object-src 'none'; base-uri 'self'"
                        ))
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(policy -> policy.policy("geolocation=(self), camera=(), microphone=(), payment=(self)"))
                )
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
