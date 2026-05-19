package org.example.springboot.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class XssRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] sanitizedBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        this.sanitizedBody = XssSanitizer.sanitize(body).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getParameter(String name) {
        return XssSanitizer.sanitize(super.getParameter(XssSanitizer.sanitize(name)));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(XssSanitizer.sanitize(name));
        if (values == null) {
            return null;
        }
        String[] sanitized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitized[i] = XssSanitizer.sanitize(values[i]);
        }
        return sanitized;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(sanitizedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
