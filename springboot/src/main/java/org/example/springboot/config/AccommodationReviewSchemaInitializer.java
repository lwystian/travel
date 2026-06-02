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
public class AccommodationReviewSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccommodationReviewSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        addColumn("review_status", "ALTER TABLE `accommodation_review` ADD COLUMN `review_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-已通过, 2-已拒绝'");
        addColumn("reviewer_id", "ALTER TABLE `accommodation_review` ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID'");
        addColumn("reviewer_name", "ALTER TABLE `accommodation_review` ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人用户名'");
        addColumn("review_time", "ALTER TABLE `accommodation_review` ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT '审核时间'");
        addColumn("review_comment", "ALTER TABLE `accommodation_review` ADD COLUMN `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见'");
    }

    private void addColumn(@NonNull String columnName, @NonNull String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'accommodation_review'
                      AND COLUMN_NAME = ?
                    """, Integer.class, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(Objects.requireNonNull(sql, "schema sql must not be null"));
            }
        } catch (Exception e) {
            LOGGER.warn("初始化住宿评价审核字段失败: {}", columnName, e);
        }
    }
}
