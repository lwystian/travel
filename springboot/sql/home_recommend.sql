-- 首页推荐表
-- 用于管理首页精选行程和更多推荐的配置

CREATE TABLE IF NOT EXISTS `home_recommend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '推荐ID',
    `type` VARCHAR(50) NOT NULL COMMENT '推荐类型: featured-精选行程, more-更多推荐',
    `tour_id` BIGINT NOT NULL COMMENT '关联的行程ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号（数字越小越靠前）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_sort_order` (`sort_order`),
    INDEX `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页推荐配置表';

-- 示例数据
-- 精选行程推荐
INSERT INTO `home_recommend` (`type`, `tour_id`, `sort_order`) VALUES
('featured', 1, 1),
('featured', 2, 2),
('featured', 3, 3);

-- 更多推荐
INSERT INTO `home_recommend` (`type`, `tour_id`, `sort_order`) VALUES
('more', 4, 1),
('more', 5, 2),
('more', 6, 3),
('more', 7, 4),
('more', 8, 5);
