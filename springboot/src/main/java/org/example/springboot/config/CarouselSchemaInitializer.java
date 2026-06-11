package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CarouselSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarouselSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        addColumn("carousel", "link_url", "ALTER TABLE `carousel` ADD COLUMN `link_url` VARCHAR(500) DEFAULT NULL COMMENT '点击跳转链接' AFTER `image_url`");
    }

    private void addColumn(String tableName, String columnName, String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, Integer.class, tableName, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(Objects.requireNonNull(sql, "schema sql must not be null"));
            }
        } catch (Exception e) {
            LOGGER.warn("Initialize column failed: {}.{}", tableName, columnName, e);
        }
    }
}
