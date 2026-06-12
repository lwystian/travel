package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CouponSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        createCouponTables();
        addOrderCouponColumns();
    }

    private void createCouponTables() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `coupon` (
                      `id` BIGINT NOT NULL AUTO_INCREMENT,
                      `name` VARCHAR(120) NOT NULL COMMENT '优惠券名称',
                      `code` VARCHAR(60) NOT NULL COMMENT '券码/批次码',
                      `description` VARCHAR(500) DEFAULT NULL COMMENT '使用说明',
                      `discount_type` VARCHAR(20) NOT NULL COMMENT 'AMOUNT满减 RATE折扣，年龄范围由min_age/max_age控制',
                      `discount_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '固定减免金额',
                      `discount_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '折扣比例，如0.85',
                      `max_discount_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '折扣封顶金额',
                      `min_order_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '最低订单金额',
                      `min_age` INT DEFAULT NULL COMMENT '最低适用年龄，空为不限',
                      `max_age` INT DEFAULT NULL COMMENT '最高适用年龄，空为不限',
                      `scope_type` VARCHAR(30) NOT NULL DEFAULT 'ALL_TOUR' COMMENT 'ALL_TOUR全部行程 TOUR指定行程 TOUR_PACKAGE指定套餐 TOUR_TYPE行程类型',
                      `scope_ids` TEXT DEFAULT NULL COMMENT '适用范围ID/编码，逗号分隔',
                      `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '总发行量，0不限量',
                      `issued_quantity` INT NOT NULL DEFAULT 0 COMMENT '已发放数量',
                      `used_quantity` INT NOT NULL DEFAULT 0 COMMENT '已使用数量',
                      `per_user_limit` INT NOT NULL DEFAULT 1 COMMENT '每用户限领',
                      `receive_start_time` DATETIME DEFAULT NULL COMMENT '领取开始时间',
                      `receive_end_time` DATETIME DEFAULT NULL COMMENT '领取结束时间',
                      `valid_start_time` DATETIME DEFAULT NULL COMMENT '有效期开始，空为不限',
                      `valid_end_time` DATETIME DEFAULT NULL COMMENT '有效期结束，空为不限',
                      `stackable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否可叠加',
                      `auto_receive` TINYINT NOT NULL DEFAULT 1 COMMENT '是否前台可领取',
                      `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
                      `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_coupon_code` (`code`),
                      INDEX `idx_coupon_status` (`status`, `valid_end_time`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表'
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `coupon_user` (
                      `id` BIGINT NOT NULL AUTO_INCREMENT,
                      `coupon_id` BIGINT NOT NULL,
                      `user_id` BIGINT NOT NULL,
                      `coupon_name` VARCHAR(120) NOT NULL,
                      `coupon_code` VARCHAR(60) NOT NULL,
                      `discount_type` VARCHAR(20) NOT NULL,
                      `discount_amount` DECIMAL(10,2) DEFAULT NULL,
                      `discount_rate` DECIMAL(5,2) DEFAULT NULL,
                      `max_discount_amount` DECIMAL(10,2) DEFAULT NULL,
                      `min_order_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
                      `min_age` INT DEFAULT NULL,
                      `max_age` INT DEFAULT NULL,
                      `scope_type` VARCHAR(30) NOT NULL DEFAULT 'ALL_TOUR',
                      `scope_ids` TEXT DEFAULT NULL,
                      `valid_start_time` DATETIME DEFAULT NULL,
                      `valid_end_time` DATETIME DEFAULT NULL,
                      `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已锁定 2已使用 3已过期 4已作废',
                      `order_id` BIGINT DEFAULT NULL,
                      `order_no` VARCHAR(80) DEFAULT NULL,
                      `used_amount` DECIMAL(10,2) DEFAULT NULL,
                      `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `lock_time` DATETIME DEFAULT NULL,
                      `use_time` DATETIME DEFAULT NULL,
                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      INDEX `idx_coupon_user_user` (`user_id`, `status`, `valid_end_time`),
                      INDEX `idx_coupon_user_coupon` (`coupon_id`, `user_id`),
                      INDEX `idx_coupon_user_order` (`order_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表'
                    """);
        } catch (Exception e) {
            LOGGER.warn("Initialize coupon tables failed", e);
        }
    }

    private void addOrderCouponColumns() {
        addColumn("tour_order", "coupon_user_id", "ALTER TABLE `tour_order` ADD COLUMN `coupon_user_id` BIGINT DEFAULT NULL COMMENT '用户优惠券ID' AFTER `hotel_amount`");
        addColumn("tour_order", "coupon_name", "ALTER TABLE `tour_order` ADD COLUMN `coupon_name` VARCHAR(120) DEFAULT NULL COMMENT '优惠券名称' AFTER `coupon_user_id`");
        addColumn("tour_order", "discount_amount", "ALTER TABLE `tour_order` ADD COLUMN `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额' AFTER `coupon_name`");
        addColumn("tour_order", "payable_amount", "ALTER TABLE `tour_order` ADD COLUMN `payable_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '应付金额' AFTER `discount_amount`");
        addColumn("coupon", "min_age", "ALTER TABLE `coupon` ADD COLUMN `min_age` INT DEFAULT NULL COMMENT '最低适用年龄，空为不限' AFTER `min_order_amount`");
        addColumn("coupon", "max_age", "ALTER TABLE `coupon` ADD COLUMN `max_age` INT DEFAULT NULL COMMENT '最高适用年龄，空为不限' AFTER `min_age`");
        addColumn("coupon_user", "min_age", "ALTER TABLE `coupon_user` ADD COLUMN `min_age` INT DEFAULT NULL AFTER `min_order_amount`");
        addColumn("coupon_user", "max_age", "ALTER TABLE `coupon_user` ADD COLUMN `max_age` INT DEFAULT NULL AFTER `min_age`");
        addColumn("coupon", "deleted", "ALTER TABLE `coupon` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除' AFTER `status`");
        relaxNullable("coupon", "valid_start_time", "ALTER TABLE `coupon` MODIFY COLUMN `valid_start_time` DATETIME DEFAULT NULL COMMENT '有效期开始，空为不限'");
        relaxNullable("coupon", "valid_end_time", "ALTER TABLE `coupon` MODIFY COLUMN `valid_end_time` DATETIME DEFAULT NULL COMMENT '有效期结束，空为不限'");
        relaxNullable("coupon_user", "valid_start_time", "ALTER TABLE `coupon_user` MODIFY COLUMN `valid_start_time` DATETIME DEFAULT NULL");
        relaxNullable("coupon_user", "valid_end_time", "ALTER TABLE `coupon_user` MODIFY COLUMN `valid_end_time` DATETIME DEFAULT NULL");
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
                jdbcTemplate.execute(Objects.requireNonNull(sql));
            }
        } catch (Exception e) {
            LOGGER.warn("Initialize column failed: {}.{}", tableName, columnName, e);
        }
    }

    private void relaxNullable(String tableName, String columnName, String sql) {
        try {
            String nullable = jdbcTemplate.queryForObject("""
                    SELECT IS_NULLABLE
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, String.class, tableName, columnName);
            if ("NO".equalsIgnoreCase(Objects.toString(nullable, ""))) {
                jdbcTemplate.execute(Objects.requireNonNull(sql));
            }
        } catch (Exception e) {
            LOGGER.warn("Relax nullable failed: {}.{}", tableName, columnName, e);
        }
    }
}
