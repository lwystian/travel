package org.example.springboot.security;

import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.service.AdminPermissionService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public final class SecurityGuards implements ApplicationContextAware {
    private static AdminPermissionService adminPermissionService;

    public SecurityGuards() {
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        adminPermissionService = applicationContext.getBean(AdminPermissionService.class);
    }

    public static User requireAdmin() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!RolePermission.isAdmin(currentUser)) {
            throw new ServiceException("权限不足，请联系管理员");
        }
        return currentUser;
    }

    public static User requireSuperAdmin() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!RolePermission.isSuperAdmin(currentUser)) {
            throw new ServiceException("权限不足，请联系管理员");
        }
        return currentUser;
    }

    public static User requirePermission(String permission) {
        User currentUser = requireAdmin();
        if (adminPermissionService != null && !adminPermissionService.hasPermission(currentUser, permission)) {
            throw new ServiceException("权限不足，请联系管理员");
        }
        return currentUser;
    }

    public static User requireAnyPermission(String... permissions) {
        User currentUser = requireAdmin();
        if (RolePermission.isSuperAdmin(currentUser) || permissions == null || permissions.length == 0 || adminPermissionService == null) {
            return currentUser;
        }
        for (String permission : permissions) {
            if (adminPermissionService.hasPermission(currentUser, permission)) {
                return currentUser;
            }
        }
        throw new ServiceException("权限不足，请联系管理员");
    }
}
