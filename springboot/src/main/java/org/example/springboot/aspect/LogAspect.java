package org.example.springboot.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.springboot.util.LogSanitizer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class LogAspect {
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;

    @Pointcut("execution(* org.example.springboot.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return point.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();
        long startTime = System.currentTimeMillis();
        String operation = resolveOperation(point);

        try {
            Object result = point.proceed();
            long duration = System.currentTimeMillis() - startTime;
            int status = response != null ? response.getStatus() : HttpServletResponse.SC_OK;
            writeAccessLog(request, operation, status, duration, null);
            return result;
        } catch (Throwable throwable) {
            long duration = System.currentTimeMillis() - startTime;
            int status = response != null ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            if (status < HttpServletResponse.SC_BAD_REQUEST) {
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            writeAccessLog(request, operation, status, duration, throwable);
            throw throwable;
        }
    }

    private String resolveOperation(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private void writeAccessLog(HttpServletRequest request, String operation, int status, long duration, Throwable throwable) {
        String message = "http_access method={} path={} status={} durationMs={} ip={} operation={} userAgent={}";
        Object[] args = {
                request.getMethod(),
                request.getRequestURI(),
                status,
                duration,
                getClientIp(request),
                operation,
                LogSanitizer.truncate(request.getHeader("User-Agent"), 300)
        };

        if (throwable != null) {
            Object[] errorArgs = append(append(args, LogSanitizer.sanitizeThrowable(throwable)), throwable);
            log.error(message + " error={}", errorArgs);
        } else if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            log.error(message, args);
        } else if (status >= HttpServletResponse.SC_BAD_REQUEST || duration >= SLOW_REQUEST_THRESHOLD_MS) {
            log.warn(message, args);
        } else {
            log.info(message, args);
        }
    }

    private Object[] append(Object[] args, Object value) {
        Object[] result = new Object[args.length + 1];
        System.arraycopy(args, 0, result, 0, args.length);
        result[args.length] = value;
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = firstNonBlank(
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getHeader("Proxy-Client-IP"),
                request.getHeader("WL-Proxy-Client-IP"),
                request.getRemoteAddr()
        );
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "unknown";
    }
}
