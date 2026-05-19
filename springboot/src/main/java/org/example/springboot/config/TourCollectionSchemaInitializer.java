package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TourCollectionSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TourCollectionSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `tour_collection` (
                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
                      `user_id` BIGINT NOT NULL COMMENT '用户ID',
                      `tour_id` BIGINT NOT NULL COMMENT '行程ID',
                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_tour_collection_user_tour` (`user_id`, `tour_id`),
                      KEY `idx_tour_collection_user` (`user_id`),
                      KEY `idx_tour_collection_tour` (`tour_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程收藏表'
                    """);
        } catch (Exception e) {
            LOGGER.warn("初始化行程收藏表失败", e);
        }
    }
}
