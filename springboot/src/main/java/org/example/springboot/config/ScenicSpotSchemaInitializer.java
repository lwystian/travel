package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ScenicSpotSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenicSpotSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        addColumn("tags", "ALTER TABLE `scenic_spot` ADD COLUMN `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，多个标签用空格分隔' AFTER `latitude`");
    }

    private void addColumn(@NonNull String columnName, @NonNull String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'scenic_spot'
                      AND COLUMN_NAME = ?
                    """, Integer.class, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(Objects.requireNonNull(sql, "schema sql must not be null"));
            }
        } catch (Exception e) {
            LOGGER.warn("初始化景点字段失败: {}", columnName, e);
        }
    }
}
