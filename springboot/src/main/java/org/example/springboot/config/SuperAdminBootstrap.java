package org.example.springboot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SuperAdminBootstrap implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperAdminBootstrap.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.bootstrap-enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.super-admin.username:superadmin}")
    private String username;

    @Value("${app.super-admin.phone:15310614553}")
    private String phone;

    @Value("${app.super-admin.initial-password:Admin@123456}")
    private String initialPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            return;
        }

        User root = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (root != null) {
            normalizeRootAccount(root, true);
            return;
        }

        User existingSuperAdmin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, RolePermission.SUPER_ADMIN)
                .orderByDesc(User::getStatus)
                .orderByAsc(User::getId)
                .last("LIMIT 1"));
        if (existingSuperAdmin != null) {
            existingSuperAdmin.setUsername(username);
            normalizeRootAccount(existingSuperAdmin, true);
            LOGGER.warn("Renamed existing SUPER_ADMIN id={} to configured root username={}.", existingSuperAdmin.getId(), username);
            return;
        }

        clearRootPhoneFromOthers(null);
        User user = new User();
        user.setUsername(username);
        user.setNickname("超级管理员");
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setRoleCode(RolePermission.SUPER_ADMIN);
        user.setStatus(1);
        user.setLoginCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        LOGGER.warn("No active admin account found. Created emergency SUPER_ADMIN account. username={}, phone={}. Change the initial password immediately.", username, phone);
    }

    private void normalizeRootAccount(User root, boolean resetPasswordWhenBlank) {
        clearRootPhoneFromOthers(root.getId());
        root.setPhone(phone);
        root.setRoleCode(RolePermission.SUPER_ADMIN);
        root.setStatus(1);
        if (isBlank(root.getNickname())) {
            root.setNickname("超级管理员");
        }
        if (resetPasswordWhenBlank && isBlank(root.getPassword())) {
            root.setPassword(passwordEncoder.encode(initialPassword));
        }
        root.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(root);
        demoteOtherSuperAdmins(root.getId());
    }

    private void demoteOtherSuperAdmins(Long keeperId) {
        List<User> superAdmins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, RolePermission.SUPER_ADMIN)
                .orderByDesc(User::getStatus)
                .orderByAsc(User::getId));
        if (superAdmins == null || superAdmins.isEmpty()) {
            return;
        }
        for (User duplicate : superAdmins) {
            if (duplicate.getId() != null && duplicate.getId().equals(keeperId)) {
                continue;
            }
            duplicate.setRoleCode(RolePermission.ADMIN);
            duplicate.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(duplicate);
            LOGGER.warn("Duplicate SUPER_ADMIN found. Kept user id={}, demoted user id={} to ADMIN.", keeperId, duplicate.getId());
        }
    }

    private void clearRootPhoneFromOthers(Long ownerId) {
        if (isBlank(phone)) {
            return;
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getPhone, phone)
                .set(User::getPhone, null)
                .set(User::getUpdateTime, LocalDateTime.now());
        if (ownerId != null) {
            wrapper.ne(User::getId, ownerId);
        }
        userMapper.update(null, wrapper);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
