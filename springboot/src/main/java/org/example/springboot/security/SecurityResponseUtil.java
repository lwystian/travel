package org.example.springboot.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

final class SecurityResponseUtil {
    private SecurityResponseUtil() {
    }

    static void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"403\",\"msg\":\"" + message + "\"}");
    }
}
