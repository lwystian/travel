package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ContentMetadataSchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ContentMetadataSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        ensureContentMetadataColumns("comment", "create_time");
        ensureContentMetadataColumns("accommodation_review", "review_comment");
        ensureContentMetadataColumns("travel_guide", "update_time");
    }

    private void ensureContentMetadataColumns(String tableName, String afterColumn) {
        addColumn(tableName, "ip_address", String.format("ALTER TABLE `%s` ADD COLUMN `ip_address` VARCHAR(100) DEFAULT NULL COMMENT 'IP地址' AFTER `%s`", tableName, afterColumn));
        addColumn(tableName, "port", String.format("ALTER TABLE `%s` ADD COLUMN `port` INT DEFAULT NULL COMMENT '网络源端口' AFTER `ip_address`", tableName));
        addColumn(tableName, "user_agent", String.format("ALTER TABLE `%s` ADD COLUMN `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息' AFTER `port`", tableName));
        addColumn(tableName, "device_id", String.format("ALTER TABLE `%s` ADD COLUMN `device_id` VARCHAR(128) DEFAULT NULL COMMENT '设备ID' AFTER `user_agent`", tableName));
        addColumn(tableName, "device_fingerprint", String.format("ALTER TABLE `%s` ADD COLUMN `device_fingerprint` VARCHAR(255) DEFAULT NULL COMMENT '设备指纹' AFTER `device_id`", tableName));
        addColumn(tableName, "client_hardware", String.format("ALTER TABLE `%s` ADD COLUMN `client_hardware` VARCHAR(500) DEFAULT NULL COMMENT '客户端硬件特征' AFTER `device_fingerprint`", tableName));
        addColumn(tableName, "mac_address", String.format("ALTER TABLE `%s` ADD COLUMN `mac_address` VARCHAR(64) DEFAULT NULL COMMENT 'MAC地址' AFTER `client_hardware`", tableName));
    }

    private void addColumn(String tableName, String columnName, String sql) {
        String ddl = Objects.requireNonNull(sql, "sql");
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, Integer.class, tableName, columnName);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
            logger.info("{}.{} column initialized", tableName, columnName);
        } catch (Exception e) {
            logger.warn("Failed to initialize {}.{} column", tableName, columnName, e);
        }
    }
}
