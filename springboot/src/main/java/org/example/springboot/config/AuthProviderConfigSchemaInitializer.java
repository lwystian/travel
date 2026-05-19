package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthProviderConfigSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthProviderConfigSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `auth_provider_config` (
                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
                      `config_type` VARCHAR(40) NOT NULL COMMENT '配置类型',
                      `config_name` VARCHAR(80) NOT NULL COMMENT '配置名称',
                      `enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
                      `config_data` TEXT COMMENT '配置数据',
                      `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
                      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_auth_provider_type` (`config_type`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证服务配置表'
                    """);

            ensureDefault("aliyun_sms", "阿里云短信", "用于登录注册短信验证码发送");
            ensureDefault("geetest", "极验验证", "用于短信发送前的人机验证");
        } catch (Exception e) {
            LOGGER.warn("初始化认证配置表失败", e);
        }
    }

    private void ensureDefault(String type, String name, String description) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_provider_config WHERE config_type = ?",
                Integer.class,
                type
        );
        if (count != null && count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO auth_provider_config(config_type, config_name, enabled, config_data, description)
                    VALUES (?, ?, 0, '{}', ?)
                    """, type, name, description);
        }
    }
}
