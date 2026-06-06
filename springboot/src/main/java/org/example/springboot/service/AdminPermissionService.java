package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.AdminPermission;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AdminPermissionMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.AdminPermissionCatalog;
import org.example.springboot.security.RolePermission;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminPermissionService {
    @Resource
    private AdminPermissionMapper adminPermissionMapper;
    @Resource
    private UserMapper userMapper;

    public List<AdminPermissionCatalog.PermissionItem> catalog() {
        return AdminPermissionCatalog.ITEMS;
    }

    public List<String> resolvePermissions(User user) {
        if (user == null) {
            return RolePermission.permissionsOf(RolePermission.USER);
        }
        String roleCode = RolePermission.normalizeRole(user.getRoleCode());
        if (RolePermission.SUPER_ADMIN.equals(roleCode)) {
            return AdminPermissionCatalog.allCodes();
        }
        if (!RolePermission.ADMIN.equals(roleCode)) {
            return RolePermission.permissionsOf(roleCode);
        }
        if (!hasCustomPermissions(user.getId())) {
            return expandLegacyPermissions(RolePermission.permissionsOf(roleCode));
        }
        return expandLegacyPermissions(getPermissionCodes(user.getId()));
    }

    public boolean hasPermission(User user, String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        if (RolePermission.isSuperAdmin(user)) {
            return true;
        }
        return resolvePermissions(user).contains(permission);
    }

    public List<User> listAdministrators() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getRoleCode, List.of(RolePermission.SUPER_ADMIN, RolePermission.ADMIN))
                .last("ORDER BY CASE WHEN role_code = 'SUPER_ADMIN' THEN 0 WHEN role_code = 'ADMIN' THEN 1 ELSE 2 END, id DESC"));
        users.forEach(user -> {
            String roleCode = RolePermission.normalizeRole(user.getRoleCode());
            user.setRoleCode(roleCode);
            user.setRoleName(RolePermission.roleNameOf(roleCode));
            user.setPermissions(resolvePermissions(user));
            user.setSuperAdmin(RolePermission.SUPER_ADMIN.equals(roleCode));
            user.setProtectedAccount(Boolean.TRUE.equals(user.getSuperAdmin()));
            user.setPassword(null);
        });
        return users;
    }

    public List<String> getPermissionCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return adminPermissionMapper.selectList(new LambdaQueryWrapper<AdminPermission>()
                        .eq(AdminPermission::getUserId, userId)
                        .orderByAsc(AdminPermission::getPermissionCode))
                .stream()
                .map(AdminPermission::getPermissionCode)
                .toList();
    }

    public List<String> expandLegacyPermissions(List<String> permissions) {
        Set<String> result = new LinkedHashSet<>();
        if (permissions == null) {
            return List.of();
        }
        for (String permission : permissions) {
            if (permission == null || permission.isBlank()) {
                continue;
            }
            switch (permission) {
                case "content:manage" -> result.addAll(List.of(
                        "tour:manage", "recommend:manage", "scenic:manage", "accommodation:manage",
                        "guide:manage", "comment:manage", "collection:manage", "category:manage", "carousel:manage"));
                case "system:manage" -> result.addAll(List.of(
                        "site-footer:manage", "site-assets:manage", "site-settings:manage"));
                case "review:manage" -> result.addAll(List.of("review:manage", "comment:manage"));
                default -> result.add(permission);
            }
        }
        return new ArrayList<>(result);
    }

    @Transactional
    public void savePermissions(Long userId, List<String> permissions, User actor) {
        if (!RolePermission.isSuperAdmin(actor)) {
            throw new ServiceException("只有超级管理员可以配置后台权限");
        }
        if (userId == null) {
            throw new ServiceException("管理员ID不能为空");
        }
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new ServiceException("管理员不存在");
        }
        String roleCode = RolePermission.normalizeRole(target.getRoleCode());
        if (RolePermission.SUPER_ADMIN.equals(roleCode)) {
            throw new ServiceException("超级管理员默认拥有全部权限，不需要配置");
        }
        if (!RolePermission.ADMIN.equals(roleCode)) {
            throw new ServiceException("只能配置管理员账号权限");
        }

        Set<String> normalized = new LinkedHashSet<>();
        if (permissions != null) {
            for (String permission : permissions) {
                if (permission != null && AdminPermissionCatalog.contains(permission)) {
                    normalized.add(permission);
                }
            }
        }
        normalized.add("dashboard:view");

        adminPermissionMapper.delete(new QueryWrapper<AdminPermission>().eq("user_id", userId));
        LocalDateTime now = LocalDateTime.now();
        for (String permission : normalized) {
            AdminPermission entity = new AdminPermission();
            entity.setUserId(userId);
            entity.setPermissionCode(permission);
            entity.setCreateTime(now);
            adminPermissionMapper.insert(entity);
        }
        JwtTokenUtils.updateUserCache(target);
    }

    private boolean hasCustomPermissions(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = adminPermissionMapper.selectCount(new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getUserId, userId));
        return count != null && count > 0;
    }
}
