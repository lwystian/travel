package org.example.springboot.security;

import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.util.JwtTokenUtils;

public final class SecurityGuards {
    private SecurityGuards() {
    }

    public static User requireAdmin() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!RolePermission.isAdmin(currentUser)) {
            throw new ServiceException("无权限");
        }
        return currentUser;
    }
}
