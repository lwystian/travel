package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SysOperationLogSchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SysOperationLogSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureColumns() {
        addColumn("port", "ALTER TABLE sys_operation_log ADD COLUMN port INT DEFAULT NULL COMMENT '网络源端口' AFTER ip_address");
        addColumn("user_agent", "ALTER TABLE sys_operation_log ADD COLUMN user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息' AFTER port");
        addColumn("role_code", "ALTER TABLE sys_operation_log ADD COLUMN role_code VARCHAR(30) DEFAULT 'USER' COMMENT '操作人角色编码' AFTER username");
        addColumn("role_name", "ALTER TABLE sys_operation_log ADD COLUMN role_name VARCHAR(30) DEFAULT '普通用户' COMMENT '操作人角色名称' AFTER role_code");
        ensureLogLevelColumn();
        addColumn("device_id", "ALTER TABLE sys_operation_log ADD COLUMN device_id VARCHAR(128) DEFAULT NULL COMMENT '设备ID' AFTER user_agent");
        addColumn("device_fingerprint", "ALTER TABLE sys_operation_log ADD COLUMN device_fingerprint VARCHAR(255) DEFAULT NULL COMMENT '设备指纹' AFTER device_id");
        addColumn("client_hardware", "ALTER TABLE sys_operation_log ADD COLUMN client_hardware VARCHAR(500) DEFAULT NULL COMMENT '客户端硬件特征' AFTER device_fingerprint");
        addColumn("mac_address", "ALTER TABLE sys_operation_log ADD COLUMN mac_address VARCHAR(64) DEFAULT NULL COMMENT 'MAC地址' AFTER client_hardware");
        createIndex("idx_role_code", "CREATE INDEX idx_role_code ON sys_operation_log(role_code)");
    }

    private void ensureLogLevelColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'sys_operation_log'
                      AND COLUMN_NAME = 'log_level'
                    """, Integer.class);

            if (count != null && count > 0) {
                return;
            }

            jdbcTemplate.execute("""
                    ALTER TABLE sys_operation_log
                    ADD COLUMN log_level VARCHAR(20) DEFAULT 'INFO' COMMENT '日志等级: INFO/WARN/ERROR'
                    AFTER operation_type
                    """);

            jdbcTemplate.update("""
                    UPDATE sys_operation_log
                    SET log_level = CASE
                        WHEN error_message IS NOT NULL AND error_message <> '' THEN 'ERROR'
                        WHEN status = 0 THEN 'WARN'
                        ELSE 'INFO'
                    END
                    WHERE log_level IS NULL OR log_level = ''
                    """);

            try {
                jdbcTemplate.execute("CREATE INDEX idx_log_level ON sys_operation_log(log_level)");
            } catch (Exception indexException) {
                logger.debug("sys_operation_log.log_level index already exists or cannot be created", indexException);
            }

            logger.info("sys_operation_log.log_level column initialized");
        } catch (Exception e) {
            logger.warn("Failed to initialize sys_operation_log.log_level column. Please run db/migration/sys_operation_log_add_level.sql manually.", e);
        }
    }

    private void addColumn(String columnName, String sql) {
        String ddl = Objects.requireNonNull(sql, "sql");
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'sys_operation_log'
                      AND COLUMN_NAME = ?
                    """, Integer.class, columnName);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
            logger.info("sys_operation_log.{} column initialized", columnName);
        } catch (Exception e) {
            logger.warn("Failed to initialize sys_operation_log.{} column", columnName, e);
        }
    }

    private void createIndex(String indexName, String sql) {
        String ddl = Objects.requireNonNull(sql, "sql");
        try {
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            logger.debug("sys_operation_log.{} index already exists or cannot be created", indexName, e);
        }
    }
}
