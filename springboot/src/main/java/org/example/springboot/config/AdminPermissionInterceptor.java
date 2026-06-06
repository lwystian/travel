package org.example.springboot.config;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.entity.User;
import org.example.springboot.security.RolePermission;
import org.example.springboot.service.AdminPermissionService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AdminPermissionInterceptor implements HandlerInterceptor {
    @Resource
    private AdminPermissionService adminPermissionService;

    private static final Map<String, String> PATH_PERMISSIONS = new LinkedHashMap<>();

    static {
        PATH_PERMISSIONS.put("/api/user/page", "user:view");
        PATH_PERMISSIONS.put("/api/user/all", "user:view");
        PATH_PERMISSIONS.put("/api/user/role", "user:view");
        PATH_PERMISSIONS.put("/api/user/add", "user:create");
        PATH_PERMISSIONS.put("/api/user/delete", "user:delete");
        PATH_PERMISSIONS.put("/api/user/deleteBatch", "user:delete");
        PATH_PERMISSIONS.put("/api/user/status", "user:update");
        PATH_PERMISSIONS.put("/api/user/resetPassword", "user:reset-password");
        PATH_PERMISSIONS.put("/api/review/comment/unified", "comment:manage");
        PATH_PERMISSIONS.put("/api/review", "review:manage");
        PATH_PERMISSIONS.put("/api/sensitive-word", "review:manage");
        PATH_PERMISSIONS.put("/api/comment/deleteBatch", "comment:manage");
        PATH_PERMISSIONS.put("/api/comment/backend", "comment:manage");
        PATH_PERMISSIONS.put("/api/scenic", "scenic:manage");
        PATH_PERMISSIONS.put("/api/scenic-category", "category:manage");
        PATH_PERMISSIONS.put("/api/category", "category:manage");
        PATH_PERMISSIONS.put("/api/guide", "guide:manage");
        PATH_PERMISSIONS.put("/api/tour/recommends", "recommend:manage");
        PATH_PERMISSIONS.put("/api/tour/recommend", "recommend:manage");
        PATH_PERMISSIONS.put("/api/tour", "tour:manage");
        PATH_PERMISSIONS.put("/api/tour-detail", "tour:manage");
        PATH_PERMISSIONS.put("/api/tour-hotel", "tour:manage");
        PATH_PERMISSIONS.put("/api/accommodation", "accommodation:manage");
        PATH_PERMISSIONS.put("/api/carousel", "carousel:manage");
        PATH_PERMISSIONS.put("/api/recommend", "recommend:manage");
        PATH_PERMISSIONS.put("/api/collection/admin", "collection:manage");
        PATH_PERMISSIONS.put("/api/notification/admin", "notification:manage");
        PATH_PERMISSIONS.put("/api/payment", "payment:manage");
        PATH_PERMISSIONS.put("/api/payment-config", "payment:manage");
        PATH_PERMISSIONS.put("/api/auth/config", "auth-config:manage");
        PATH_PERMISSIONS.put("/api/admin-governance", "permission:manage");
        PATH_PERMISSIONS.put("/api/site/footer", "site-footer:manage");
        PATH_PERMISSIONS.put("/api/site/assets", "site-assets:manage");
        PATH_PERMISSIONS.put("/api/site/access", "site-settings:manage");
        PATH_PERMISSIONS.put("/api/site/page-content", "site-settings:manage");
        PATH_PERMISSIONS.put("/api/admin/logs", "log:view");
        PATH_PERMISSIONS.put("/api/log", "log:view");
        PATH_PERMISSIONS.put("/api/sys/log", "log:view");
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String permission = requiredPermission(request);
        if (permission == null) {
            return true;
        }
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!RolePermission.isAdmin(currentUser) || !hasAnyPermission(currentUser, permission)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"code\":\"403\",\"msg\":\"权限不足，请联系管理员\"}");
            return false;
        }
        return true;
    }

    private boolean hasAnyPermission(User currentUser, String permissionExpression) {
        String[] permissions = permissionExpression.split("\\|");
        for (String permission : permissions) {
            if (adminPermissionService.hasPermission(currentUser, permission)) {
                return true;
            }
        }
        return false;
    }

    private String requiredPermission(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String orderPermission = orderPermission(path, method);
        if (orderPermission != null) {
            return orderPermission;
        }
        if ("GET".equalsIgnoreCase(method) && isPublicFrontendGet(path)) {
            return null;
        }
        String crossPagePermission = auxiliaryReadPermission(request);
        if (crossPagePermission != null) {
            return crossPagePermission;
        }
        if (path.startsWith("/api/guide/add")
                || path.startsWith("/api/guide/update")
                || path.startsWith("/api/guide/delete")
                || path.startsWith("/api/guide/my")) {
            return null;
        }
        if (path.startsWith("/api/accommodation/review")) {
            return null;
        }
        for (Map.Entry<String, String> entry : PATH_PERMISSIONS.entrySet()) {
            if (path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String orderPermission(String path, String method) {
        if (path.equals("/api/tour-order/all") && "GET".equalsIgnoreCase(method)) {
            return "order:manage";
        }
        if (path.startsWith("/api/tour-order/") && path.endsWith("/refund") && "PUT".equalsIgnoreCase(method)) {
            return "order:manage";
        }
        if (path.startsWith("/api/tour-order/") && "DELETE".equalsIgnoreCase(method)) {
            return "order:manage";
        }
        if (path.startsWith("/api/tour-order-pay/mock-pay/") && "POST".equalsIgnoreCase(method)) {
            return "order:manage";
        }
        return null;
    }

    private String auxiliaryReadPermission(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            return null;
        }
        if (path.equals("/api/tour/recommends")) {
            return "recommend:manage";
        }
        if (path.equals("/api/tour/all")) {
            return "tour:manage|recommend:manage";
        }
        if (path.equals("/api/tour/page") && "true".equalsIgnoreCase(request.getParameter("includeInactive"))) {
            return "tour:manage";
        }
        return null;
    }

    private boolean isPublicFrontendGet(String path) {
        return path.equals("/api/carousel/active")
                || path.equals("/api/payment-config/enabled")
                || path.equals("/api/tour/list")
                || path.equals("/api/tour/featured")
                || path.equals("/api/tour/more")
                || path.equals("/api/tour/filters")
                || path.equals("/api/tour/hot-keywords")
                || path.equals("/api/tour/ticket-featured")
                || path.equals("/api/tour/recommended")
                || path.equals("/api/tour-order-pay/methods")
                || path.startsWith("/api/site/access/public")
                || path.startsWith("/api/site/assets/public")
                || path.startsWith("/api/site/footer/public")
                || path.startsWith("/api/site/page-content/public")
                || path.startsWith("/api/scenic")
                || path.startsWith("/api/accommodation")
                || path.startsWith("/api/guide/page")
                || path.startsWith("/api/guide/")
                || path.startsWith("/api/comment/page")
                || path.startsWith("/api/comment/scenic/");
    }
}
