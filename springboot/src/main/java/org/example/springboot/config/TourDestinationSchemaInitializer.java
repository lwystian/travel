package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TourDestinationSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TourDestinationSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.update("UPDATE `tour` SET `destination` = 'sanxia' WHERE `destination` = 'sanyan'");
        } catch (Exception e) {
            LOGGER.warn("修正三峡邮轮目的地代码失败", e);
        }
    }
}
