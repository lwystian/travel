package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserAuthSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            ensureEmailNullable();
            ensurePhoneColumn();
            ensurePhoneUniqueIndex();
        } catch (Exception e) {
            LOGGER.warn("初始化手机号认证用户表结构失败，请检查 user.email 是否可空、user.phone 是否存在唯一索引", e);
        }
    }

    private void ensureEmailNullable() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'user'
                  AND COLUMN_NAME = 'email'
                  AND IS_NULLABLE = 'NO'
                """, Integer.class);
        if (count != null && count > 0) {
            jdbcTemplate.execute("ALTER TABLE `user` MODIFY COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱'");
            LOGGER.info("user.email 已调整为可空，以适配手机号注册");
        }
    }

    private void ensurePhoneColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'user'
                  AND COLUMN_NAME = 'phone'
                """, Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `email`");
            LOGGER.info("user.phone 字段已创建");
        }
    }

    private void ensurePhoneUniqueIndex() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'user'
                  AND INDEX_NAME = 'uk_user_phone'
                """, Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.execute("CREATE UNIQUE INDEX uk_user_phone ON `user`(`phone`)");
            LOGGER.info("user.phone 唯一索引已创建");
        }
    }
}
