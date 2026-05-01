-- 创建景点标签表
CREATE TABLE `scenic_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `color` VARCHAR(20) DEFAULT '#FF6B35' COMMENT '标签颜色',
  `icon` VARCHAR(50) DEFAULT '' COMMENT '标签图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点标签表';

-- 创建景点与标签关联表
CREATE TABLE `scenic_spot_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scenic_spot_id` BIGINT NOT NULL COMMENT '景点ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  KEY `idx_scenic_spot_id` (`scenic_spot_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点标签关联表';

-- 插入初始标签数据
INSERT INTO `scenic_tag` (`name`, `color`, `sort_order`) VALUES
('文化古迹', '#8B5CF6', 1),
('拍照圣地', '#EC4899', 2),
('快速出票', '#10B981', 3),
('热门推荐', '#F59E0B', 4),
('亲子友好', '#06B6D4', 5),
('自然风光', '#22C55E', 6),
('历史遗迹', '#6366F1', 7),
('网红打卡', '#EF4444', 8);

-- 为现有景点添加标签关联
INSERT INTO `scenic_spot_tag` (`scenic_spot_id`, `tag_id`) VALUES
(1, 1), (1, 2), (1, 3),
(2, 4), (2, 7),
(3, 2), (3, 8),
(4, 5), (4, 6),
(5, 1), (5, 4),
(6, 2), (6, 3), (6, 8),
(7, 6), (7, 7),
(8, 3), (8, 5),
(9, 1), (9, 8),
(10, 4), (10, 6);
