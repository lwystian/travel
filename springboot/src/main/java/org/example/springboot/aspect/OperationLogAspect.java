package org.example.springboot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.entity.SysOperationLog;
import org.example.springboot.entity.User;
import org.example.springboot.service.SysOperationLogService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 * 自动拦截标注了@OperationLog注解的方法，记录操作日志
 */
@Aspect
@Component
public class OperationLogAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);
    
    @Resource
    private SysOperationLogService sysOperationLogService;
    
    @Resource
    private ObjectMapper objectMapper;
    
    /**
     * 定义切点：标注了@OperationLog注解的方法
     */
    @Pointcut("@annotation(org.example.springboot.annotation.OperationLog)")
    public void operationLogPointcut() {}
    
    /**
     * 环绕通知：记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog log = new SysOperationLog();
        
        try {
            // 获取方法签名和注解
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperationLog operationLog = method.getAnnotation(OperationLog.class);
            
            // 设置基本信息
            log.setOperationType(operationLog.operationType());
            log.setOperationDesc(operationLog.description());
            log.setRequestMethod(getRequestMethod());
            log.setRequestUrl(getRequestUrl());
            log.setIpAddress(getClientIp());
            log.setPort(getClientPort());
            log.setUserAgent(getUserAgent());
            
            // 获取当前用户信息
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                log.setUserId(currentUser.getId());
                log.setUsername(currentUser.getUsername());
            }
            
            // 设置目标对象信息
            log.setTargetType(joinPoint.getTarget().getClass().getSimpleName().replace("Controller", ""));
            log.setTargetId(extractTargetId(joinPoint.getArgs(), signature.getParameterNames()));
            
            // 记录请求参数
            if (operationLog.logParams()) {
                log.setRequestParams(extractParams(joinPoint.getArgs(), signature.getParameterNames()));
            }
            
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 设置执行结果
            log.setStatus(1); // 成功
            log.setExecutionTime(System.currentTimeMillis() - startTime);
            
            // 异步保存日志
            sysOperationLogService.saveLogAsync(log);
            
            return result;
            
        } catch (Exception e) {
            // 记录异常信息
            log.setStatus(0); // 失败
            log.setErrorMessage(e.getMessage());
            log.setExecutionTime(System.currentTimeMillis() - startTime);
            
            // 异步保存日志
            sysOperationLogService.saveLogAsync(log);
            
            throw e;
        }
    }
    
    /**
     * 获取当前请求的IP地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_CLIENT_IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip;
            }
        } catch (Exception e) {
            logger.error("获取客户端IP异常", e);
        }
        return "unknown";
    }
    
    /**
     * 获取当前请求的端口
     */
    private int getClientPort() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getRemotePort();
            }
        } catch (Exception e) {
            logger.error("获取客户端端口异常", e);
        }
        return 0;
    }
    
    /**
     * 获取当前请求的UserAgent
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                String userAgent = requestAttributes.getRequest().getHeader("User-Agent");
                return userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
            }
        } catch (Exception e) {
            logger.error("获取UserAgent异常", e);
        }
        return "";
    }
    
    /**
     * 获取当前请求的URL
     */
    private String getRequestUrl() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getRequestURI();
            }
        } catch (Exception e) {
            logger.error("获取请求URL异常", e);
        }
        return "";
    }
    
    /**
     * 获取当前请求的方法
     */
    private String getRequestMethod() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getMethod();
            }
        } catch (Exception e) {
            logger.error("获取请求方法异常", e);
        }
        return "";
    }
    
    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        try {
            return JwtTokenUtils.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 提取目标对象ID
     */
    private String extractTargetId(Object[] args, String[] paramNames) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        // 尝试从路径参数中获取ID
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Long || arg instanceof Integer) {
                return String.valueOf(arg);
            }
            if (arg instanceof String && paramNames != null && i < paramNames.length) {
                String paramName = paramNames[i].toLowerCase();
                if (paramName.contains("id")) {
                    return String.valueOf(arg);
                }
            }
        }
        return "";
    }
    
    /**
     * 提取请求参数
     */
    private String extractParams(Object[] args, String[] paramNames) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        try {
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                String paramName = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
                
                // 过滤敏感参数
                if (arg instanceof String) {
                    String strVal = (String) arg;
                    if (paramName.toLowerCase().contains("password") || 
                        paramName.toLowerCase().contains("pwd") ||
                        paramName.toLowerCase().contains("token") ||
                        paramName.toLowerCase().contains("secret") ||
                        paramName.toLowerCase().contains("key")) {
                        params.put(paramName, "***");
                    } else {
                        params.put(paramName, strVal.length() > 200 ? strVal.substring(0, 200) + "..." : strVal);
                    }
                } else if (arg != null && !isFileOrMultipart(arg)) {
                    // 序列化参数对象
                    try {
                        String json = objectMapper.writeValueAsString(arg);
                        if (json.length() > 500) {
                            json = json.substring(0, 500) + "...";
                        }
                        params.put(paramName, json);
                    } catch (Exception e) {
                        params.put(paramName, arg.toString());
                    }
                }
            }
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            logger.error("提取请求参数异常", e);
            return "";
        }
    }
    
    /**
     * 判断是否是文件上传类型
     */
    private boolean isFileOrMultipart(Object obj) {
        String className = obj.getClass().getName();
        return className.contains("MultipartFile") || 
               className.contains("UploadedFile") ||
               className.contains("Part");
    }
}
