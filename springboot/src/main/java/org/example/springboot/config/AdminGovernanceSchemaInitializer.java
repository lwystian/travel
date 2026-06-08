package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminGovernanceSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminGovernanceSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            ensureAdminPermissionTable();
            ensureContentModerationConfigTable();
            ensureDefaultConfig("admin_guide_review_required", "0", "管理员发布或编辑攻略是否需要人工审核");
            ensureDefaultConfig("admin_comment_review_required", "0", "管理员发布评论或住宿评价是否需要人工审核");
            ensureDefaultConfig("public_interaction_enabled", "1", "前台用户评论、住宿评价、攻略发布等互动内容是否开放");
        } catch (Exception e) {
            LOGGER.warn("初始化管理员权限与内容审核策略表失败", e);
        }
    }

    private void ensureAdminPermissionTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `admin_permission` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `user_id` BIGINT NOT NULL COMMENT '管理员用户ID',
                  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_admin_permission_user_code` (`user_id`, `permission_code`),
                  KEY `idx_admin_permission_user` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员后台权限配置'
                """);
    }

    private void ensureContentModerationConfigTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `content_moderation_config` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
                  `config_value` VARCHAR(50) NOT NULL DEFAULT '0' COMMENT '配置值',
                  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_content_moderation_config_key` (`config_key`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核策略配置'
                """);
    }

    private void ensureDefaultConfig(String key, String value, String description) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM `content_moderation_config`
                WHERE `config_key` = ?
                """, Integer.class, key);
        if (count != null && count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO `content_moderation_config` (`config_key`, `config_value`, `description`)
                    VALUES (?, ?, ?)
                    """, key, value, description);
        }
    }
}
