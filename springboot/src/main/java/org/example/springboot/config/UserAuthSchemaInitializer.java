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
            ensureOrderNotifyColumn();
        } catch (Exception e) {
            LOGGER.warn("初始化用户认证表结构失败，请检查 user.email、user.phone 和订单短信提醒字段配置", e);
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

    private void ensureOrderNotifyColumn() {
        if (!hasUserColumn("order_notify_enabled")) {
            jdbcTemplate.execute("""
                    ALTER TABLE `user`
                    ADD COLUMN `order_notify_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否接收订单支付通知' AFTER `phone`
                    """);
            if (hasUserColumn("order_sms_notify_enabled")) {
                jdbcTemplate.execute("""
                        UPDATE `user`
                        SET `order_notify_enabled` = `order_sms_notify_enabled`
                        """);
            }
            jdbcTemplate.execute("""
                    UPDATE `user`
                    SET `order_notify_enabled` = 1
                    WHERE `role_code` = 'SUPER_ADMIN'
                    """);
            LOGGER.info("user.order_notify_enabled 字段已创建，超级管理员默认开启订单通知");
        }
    }

    private boolean hasUserColumn(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'user'
                  AND COLUMN_NAME = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }
}
