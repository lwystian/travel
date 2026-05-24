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
import org.example.springboot.common.Result;
import org.example.springboot.entity.SysOperationLog;
import org.example.springboot.entity.User;
import org.example.springboot.service.SysOperationLogService;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Locale;

@Aspect
@Component
public class OperationLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Resource
    private SysOperationLogService sysOperationLogService;

    @Resource
    private ObjectMapper objectMapper;

    @Pointcut("execution(* org.example.springboot.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        if (!shouldAudit(request, annotation)) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        SysOperationLog auditLog = buildBaseLog(joinPoint, signature, annotation, request);

        try {
            Object result = joinPoint.proceed();
            fillResult(auditLog, result);
            return result;
        } catch (Throwable e) {
            auditLog.setStatus(0);
            auditLog.setLogLevel("ERROR");
            auditLog.setErrorMessage(LogSanitizer.sanitizeThrowable(e));
            throw e;
        } finally {
            auditLog.setExecutionTime(System.currentTimeMillis() - startTime);
            sysOperationLogService.saveLogAsync(auditLog);
        }
    }

    private SysOperationLog buildBaseLog(
            ProceedingJoinPoint joinPoint,
            MethodSignature signature,
            OperationLog annotation,
            HttpServletRequest request) {
        SysOperationLog auditLog = new SysOperationLog();
        String operationType = resolveOperationType(annotation, request);
        String rawTargetType = resolveTargetType(annotation, joinPoint);
        String targetName = humanTargetName(rawTargetType);

        auditLog.setOperationType(operationType);
        auditLog.setLogLevel("INFO");
        auditLog.setOperationDesc(resolveOperationDesc(annotation, request, operationType, targetName));
        auditLog.setRequestMethod(request != null ? request.getMethod() : "");
        auditLog.setRequestUrl(request != null ? request.getRequestURI() : "");
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setPort(request != null ? request.getRemotePort() : 0);
        auditLog.setUserAgent(getUserAgent(request));
        auditLog.setTargetType(targetName);
        auditLog.setTargetId(extractTargetId(operationType, joinPoint.getArgs(), signature.getParameterNames()));

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            auditLog.setUserId(currentUser.getId());
            auditLog.setUsername(currentUser.getUsername());
        }

        if (annotation == null || annotation.logParams()) {
            auditLog.setRequestParams(extractParams(joinPoint.getArgs(), signature.getParameterNames()));
        }

        return auditLog;
    }

    private boolean shouldAudit(HttpServletRequest request, OperationLog annotation) {
        if (request == null) {
            return false;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path != null
                && path.contains("/frequent-traveler")
                && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
            return false;
        }
        if (annotation != null) {
            return true;
        }
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private void fillResult(SysOperationLog auditLog, Object result) {
        auditLog.setStatus(1);
        auditLog.setLogLevel("INFO");
        if (result instanceof Result<?> response && !"200".equals(response.getCode())) {
            auditLog.setStatus(0);
            auditLog.setLogLevel("WARN");
            auditLog.setErrorMessage(LogSanitizer.truncate(response.getMsg(), 500));
        }
    }

    private String resolveOperationType(OperationLog annotation, HttpServletRequest request) {
        if (annotation != null && annotation.operationType() != null && !annotation.operationType().isBlank()) {
            return annotation.operationType();
        }

        String path = request != null ? request.getRequestURI().toLowerCase(Locale.ROOT) : "";
        String method = request != null ? request.getMethod() : "";
        if (path.contains("login")) {
            return "LOGIN";
        }
        if (path.contains("logout")) {
            return "LOGOUT";
        }
        if (path.contains("register")) {
            return "REGISTER";
        }
        if (path.contains("/check") || path.contains("/test")) {
            return "CHECK";
        }
        if (path.contains("/preview")) {
            return "PREVIEW";
        }
        if (path.contains("/upload")) {
            return "UPLOAD";
        }
        if (path.contains("/like")) {
            return "LIKE";
        }
        if (path.contains("/status")) {
            return "UPDATE_STATUS";
        }
        if (path.contains("resetpassword") || path.contains("password")) {
            return "PASSWORD";
        }
        if (path.contains("/notify")) {
            return "CALLBACK";
        }
        if (path.contains("pay") || path.contains("payment")) {
            return "PAY";
        }
        if (path.contains("cancel")) {
            return "CANCEL";
        }
        if (path.contains("review")) {
            return "REVIEW";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "CREATE";
        }
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return "UPDATE";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return "DELETE";
        }
        return "QUERY";
    }

    private String resolveOperationDesc(OperationLog annotation, HttpServletRequest request, String operationType, String targetName) {
        if (annotation != null && annotation.description() != null && !annotation.description().isBlank()) {
            return annotation.description();
        }
        if (request == null) {
            return "系统操作";
        }

        String path = request.getRequestURI().toLowerCase(Locale.ROOT);
        if (path.matches(".*/sensitive-word/check$")) {
            return "测试敏感词规则";
        }
        if (path.matches(".*/notification/admin/send$")) {
            return "发送站内消息";
        }
        if (path.matches(".*/payment-config/[^/]+/toggle$")) {
            String enabled = request.getParameter("enabled");
            if ("true".equalsIgnoreCase(enabled)) {
                return "启用支付配置";
            }
            if ("false".equalsIgnoreCase(enabled)) {
                return "停用支付配置";
            }
            return "调整支付配置启用状态";
        }

        return describeOperation(operationType, request.getMethod()) + targetName;
    }

    private String describeOperation(String operationType, String method) {
        if ("LOGIN".equalsIgnoreCase(operationType)) {
            return "登录";
        }
        if ("LOGOUT".equalsIgnoreCase(operationType)) {
            return "退出";
        }
        if ("REGISTER".equalsIgnoreCase(operationType)) {
            return "注册";
        }
        if ("CHECK".equalsIgnoreCase(operationType)) {
            return "测试";
        }
        if ("PREVIEW".equalsIgnoreCase(operationType)) {
            return "预览";
        }
        if ("UPLOAD".equalsIgnoreCase(operationType)) {
            return "上传";
        }
        if ("LIKE".equalsIgnoreCase(operationType)) {
            return "点赞";
        }
        if ("UPDATE_STATUS".equalsIgnoreCase(operationType)) {
            return "调整";
        }
        if ("PASSWORD".equalsIgnoreCase(operationType)) {
            return "修改密码";
        }
        if ("CALLBACK".equalsIgnoreCase(operationType)) {
            return "处理回调";
        }
        if ("PAY".equalsIgnoreCase(operationType)) {
            return "支付";
        }
        if ("CANCEL".equalsIgnoreCase(operationType)) {
            return "取消";
        }
        if ("REVIEW".equalsIgnoreCase(operationType)) {
            return "审核";
        }
        if ("CREATE".equalsIgnoreCase(operationType) || "POST".equalsIgnoreCase(method)) {
            return "新增";
        }
        if ("UPDATE".equalsIgnoreCase(operationType) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return "更新";
        }
        if ("DELETE".equalsIgnoreCase(operationType) || "DELETE".equalsIgnoreCase(method)) {
            return "删除";
        }
        if ("QUERY".equalsIgnoreCase(operationType) || "GET".equalsIgnoreCase(method)) {
            return "查询";
        }
        if ("EXPORT".equalsIgnoreCase(operationType)) {
            return "导出";
        }
        return "操作";
    }

    private String humanTargetName(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return "业务数据";
        }
        return switch (targetType) {
            case "SysLog", "系统日志" -> "系统日志";
            case "SensitiveWord", "敏感词规则" -> "敏感词规则";
            case "SensitiveLog", "敏感词日志" -> "敏感词日志";
            case "SiteNotification", "Notification", "站内消息" -> "站内消息";
            case "User", "用户" -> "用户";
            case "ScenicSpot", "景点" -> "景点";
            case "ScenicCategory", "分类" -> "分类";
            case "TravelGuide", "攻略" -> "攻略";
            case "Comment", "评论" -> "评论";
            case "Tour", "行程" -> "行程";
            case "TourOrder", "订单" -> "订单";
            case "PaymentConfig", "支付配置" -> "支付配置";
            case "Review", "内容审核" -> "内容审核";
            case "Carousel", "轮播图" -> "轮播图";
            case "Accommodation", "住宿" -> "住宿";
            case "AccommodationReview", "住宿评价" -> "住宿评价";
            case "FrequentTraveler", "常用出行人" -> "常用出行人";
            case "File", "文件" -> "文件";
            case "Collection", "收藏" -> "收藏";
            case "ScenicCollection", "景点收藏" -> "景点收藏";
            case "TourCollection", "行程收藏" -> "行程收藏";
            case "TourDetail", "行程明细" -> "行程明细";
            case "TourHotel", "行程酒店" -> "行程酒店";
            case "TourOrderPay", "订单支付" -> "订单支付";
            case "Traveler", "出行人" -> "出行人";
            default -> "业务数据";
        };
    }

    private String resolveTargetType(OperationLog annotation, ProceedingJoinPoint joinPoint) {
        if (annotation != null && annotation.targetType() != null && !annotation.targetType().isBlank()) {
            return annotation.targetType();
        }
        return joinPoint.getTarget().getClass().getSimpleName().replace("Controller", "");
    }

    private String extractTargetId(String operationType, Object[] args, String[] paramNames) {
        if (isReadOnlyOperation(operationType) || args == null || args.length == 0) {
            return "";
        }

        for (int i = 0; i < args.length; i++) {
            String paramName = paramNames != null && i < paramNames.length ? paramNames[i] : "";
            Object arg = args[i];
            if (isPaginationParam(paramName)) {
                continue;
            }
            if ((arg instanceof Long || arg instanceof Integer) && isLikelyIdParam(paramName)) {
                return String.valueOf(arg);
            }
            if (arg instanceof String && isLikelyIdParam(paramName)) {
                return String.valueOf(arg);
            }
        }
        return "";
    }

    private boolean isReadOnlyOperation(String operationType) {
        return "QUERY".equalsIgnoreCase(operationType) || "EXPORT".equalsIgnoreCase(operationType);
    }

    private boolean isPaginationParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String normalized = paramName.toLowerCase(Locale.ROOT);
        return normalized.equals("page")
                || normalized.equals("currentpage")
                || normalized.equals("pagesize")
                || normalized.equals("size")
                || normalized.equals("limit")
                || normalized.equals("offset");
    }

    private boolean isLikelyIdParam(String paramName) {
        if (paramName == null || paramName.isBlank()) {
            return false;
        }
        String normalized = paramName.toLowerCase(Locale.ROOT);
        return normalized.equals("id") || normalized.endsWith("id") || normalized.contains("id");
    }

    private String extractParams(Object[] args, String[] paramNames) {
        return LogSanitizer.serializeArgs(objectMapper, args, paramNames, 1000);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes != null ? requestAttributes.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
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

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return LogSanitizer.truncate(request.getHeader("User-Agent"), 500);
    }

    private User getCurrentUser() {
        try {
            return JwtTokenUtils.getCurrentUser();
        } catch (Exception e) {
            logger.debug("No authenticated user found for audit log");
            return null;
        }
    }
}
