package org.example.springboot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

        User existingSuperAdmin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, RolePermission.SUPER_ADMIN)
                .last("LIMIT 1"));
        if (existingSuperAdmin != null) {
            LOGGER.info("SUPER_ADMIN account already exists. Bootstrap skipped. userId={}", existingSuperAdmin.getId());
            return;
        }

        User existingUsername = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (existingUsername != null) {
            LOGGER.warn("Super admin bootstrap skipped because configured username already belongs to a non-super-admin user. username={}, userId={}", username, existingUsername.getId());
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname("Super Administrator");
        user.setPhone(resolveBootstrapPhone());
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setRoleCode(RolePermission.SUPER_ADMIN);
        user.setStatus(1);
        user.setLoginCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        LOGGER.warn("No SUPER_ADMIN account found. Created bootstrap SUPER_ADMIN account. username={}, phone={}. Change the initial password immediately.", username, user.getPhone());
    }

    private String resolveBootstrapPhone() {
        if (isBlank(phone)) {
            return null;
        }
        User existingPhone = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1"));
        if (existingPhone != null) {
            LOGGER.warn("Configured bootstrap phone is already used by userId={}. New SUPER_ADMIN will be created without phone to avoid overwriting existing data.", existingPhone.getId());
            return null;
        }
        return phone;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
