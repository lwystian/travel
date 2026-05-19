package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SysOperationLogSchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SysOperationLogSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureLogLevelColumn() {
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
}
