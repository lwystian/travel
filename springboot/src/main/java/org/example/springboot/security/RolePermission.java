package org.example.springboot.security;

import org.example.springboot.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RolePermission {
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    private static final Map<String, List<String>> PERMISSIONS = Map.of(
            SUPER_ADMIN, List.of(
                    "dashboard:view",
                    "user:view", "user:create", "user:update", "user:delete", "user:status", "user:reset-password", "user:grant-role",
                    "tour:manage", "recommend:manage", "scenic:manage", "accommodation:manage", "guide:manage",
                    "comment:manage", "collection:manage", "category:manage", "carousel:manage",
                    "review:manage", "order:manage", "coupon:manage", "payment:manage", "auth-config:manage", "log:view",
                    "notification:manage", "permission:manage", "site-footer:manage", "site-assets:manage", "site-settings:manage"
            ),
            ADMIN, List.of(
                    "dashboard:view",
                    "user:view", "user:create-basic", "user:update-basic", "user:status-basic", "user:reset-password-basic",
                    "tour:manage", "recommend:manage", "scenic:manage", "accommodation:manage", "guide:manage",
                    "comment:manage", "collection:manage", "category:manage", "carousel:manage",
                    "review:manage", "order:manage", "coupon:manage", "notification:manage"
            ),
            USER, List.of(
                    "profile:view", "profile:update", "order:self", "collection:self", "guide:self", "comment:self"
            )
    );

    private static final Map<String, String> ROLE_NAMES = Map.of(
            SUPER_ADMIN, "超级管理员",
            ADMIN, "管理员",
            USER, "普通用户"
    );

    private RolePermission() {
    }

    public static String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return USER;
        }
        String normalized = roleCode.trim().toUpperCase();
        if (Set.of(SUPER_ADMIN, ADMIN, USER).contains(normalized)) {
            return normalized;
        }
        return USER;
    }

    public static boolean isSuperAdmin(User user) {
        return user != null && SUPER_ADMIN.equals(normalizeRole(user.getRoleCode()));
    }

    public static boolean isAdmin(User user) {
        String role = user == null ? "" : normalizeRole(user.getRoleCode());
        return SUPER_ADMIN.equals(role) || ADMIN.equals(role);
    }

    public static boolean canManageRole(User actor, String targetRole) {
        String actorRole = actor == null ? "" : normalizeRole(actor.getRoleCode());
        String target = normalizeRole(targetRole);
        if (SUPER_ADMIN.equals(actorRole)) {
            return true;
        }
        return ADMIN.equals(actorRole) && USER.equals(target);
    }

    public static List<String> permissionsOf(String roleCode) {
        return PERMISSIONS.getOrDefault(normalizeRole(roleCode), PERMISSIONS.get(USER));
    }

    public static String roleNameOf(String roleCode) {
        return ROLE_NAMES.getOrDefault(normalizeRole(roleCode), ROLE_NAMES.get(USER));
    }
}
