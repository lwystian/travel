package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SiteNotificationSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SiteNotificationSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `site_notification` (
                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                      `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
                      `title` VARCHAR(120) NOT NULL COMMENT '标题',
                      `content` TEXT NOT NULL COMMENT '内容',
                      `type` VARCHAR(30) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型',
                      `business_type` VARCHAR(50) COMMENT '业务类型',
                      `business_id` VARCHAR(80) COMMENT '业务ID',
                      `link_url` VARCHAR(255) COMMENT '跳转地址',
                      `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
                      `read_time` DATETIME COMMENT '阅读时间',
                      `sender_type` VARCHAR(30) NOT NULL DEFAULT 'SYSTEM' COMMENT '发送方类型',
                      `sender_id` BIGINT COMMENT '发送方ID',
                      `sender_name` VARCHAR(80) COMMENT '发送方名称',
                      `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      PRIMARY KEY (`id`),
                      INDEX `idx_notification_user_read` (`user_id`, `read_status`),
                      INDEX `idx_notification_create_time` (`create_time`),
                      INDEX `idx_notification_business` (`business_type`, `business_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息表'
                    """);
        } catch (Exception e) {
            LOGGER.warn("初始化站内消息表失败，请检查数据库权限", e);
        }
    }
}
