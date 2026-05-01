-- 为 tour 表添加新字段（如果不存在）
ALTER TABLE `tour` ADD COLUMN `code` VARCHAR(50) DEFAULT '' COMMENT '行程编号（业务编码）' AFTER `id`;

-- 更新现有行程的编号
-- 周边游 (around) -> ZW
UPDATE `tour` SET `code` = 'ZW-001' WHERE `id` = 1;
-- 邮轮 (cruise) -> YL
UPDATE `tour` SET `code` = 'YL-001' WHERE `id` = 2;
-- 跟团游 (team) -> GT
UPDATE `tour` SET `code` = 'GT-001' WHERE `id` = 3;
-- 更多邮轮
UPDATE `tour` SET `code` = 'YL-002' WHERE `id` = 4;
UPDATE `tour` SET `code` = 'GT-002' WHERE `id` = 5;
UPDATE `tour` SET `code` = 'YL-003' WHERE `id` = 6;
UPDATE `tour` SET `code` = 'YL-004' WHERE `id` = 7;
UPDATE `tour` SET `code` = 'YL-005' WHERE `id` = 8;

-- 为 tour 表添加新字段（如果不存在）
ALTER TABLE `tour` ADD COLUMN `video_url` VARCHAR(500) DEFAULT '' COMMENT '视频URL' AFTER `status`;
ALTER TABLE `tour` ADD COLUMN `video_poster` VARCHAR(500) DEFAULT '' COMMENT '视频封面URL' AFTER `video_url`;
ALTER TABLE `tour` ADD COLUMN `video_enabled` INT DEFAULT 0 COMMENT '视频启用状态: 1-启用, 0-禁用' AFTER `video_poster`;
ALTER TABLE `tour` ADD COLUMN `notice` VARCHAR(500) DEFAULT '' COMMENT '出团通知' AFTER `video_enabled`;

-- 更新现有行程的视频和出团通知
UPDATE `tour` SET `notice` = '周边游提前1天，国内游提前3天，出境游提前3-5天，APP和短信群发出团通知' WHERE `notice` IS NULL OR `notice` = '';

-- 行程套餐表（不同价格档次的套餐）
CREATE TABLE IF NOT EXISTS `tour_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `tour_id` BIGINT NOT NULL COMMENT '关联行程ID',
  `name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `adult_price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '成人价格',
  `child_price` DECIMAL(10,2) DEFAULT 0 COMMENT '儿童价格',
  `description` VARCHAR(500) DEFAULT '' COMMENT '套餐描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` INT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程套餐表';

-- 批次套餐表（不同出发地点/服务档次的套餐）
CREATE TABLE IF NOT EXISTS `batch_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次套餐ID',
  `tour_id` BIGINT NOT NULL COMMENT '关联行程ID',
  `name` VARCHAR(100) NOT NULL COMMENT '批次套餐名称',
  `extra_fee_per_person` DECIMAL(10,2) DEFAULT 0 COMMENT '每人附加费',
  `description` VARCHAR(500) DEFAULT '' COMMENT '套餐描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` INT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次套餐表';

-- 出发日期表（班期管理）
CREATE TABLE IF NOT EXISTS `tour_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `tour_id` BIGINT NOT NULL COMMENT '关联行程ID',
  `departure_date` DATE NOT NULL COMMENT '出发日期',
  `adult_date_extra_fee` DECIMAL(10,2) DEFAULT 0 COMMENT '成人日期附加费',
  `child_date_extra_fee` DECIMAL(10,2) DEFAULT 0 COMMENT '儿童日期附加费',
  `status` VARCHAR(20) DEFAULT '可报名' COMMENT '状态: 可报名/已满员/已结束',
  `remaining` INT DEFAULT 0 COMMENT '余位',
  `max_capacity` INT DEFAULT 50 COMMENT '最大容量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tour_date` (`tour_id`, `departure_date`),
  KEY `idx_departure_date` (`departure_date`),
  KEY `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出发日期表';

-- 插入示例数据
-- 为行程ID=1 (南川风吹草原) 添加套餐
INSERT INTO `tour_package` (`tour_id`, `name`, `adult_price`, `child_price`, `description`, `sort_order`, `status`) VALUES
(1, '标准套餐', 158.00, 79.00, '包含基础服务', 1, 1),
(1, 'VIP套餐', 198.00, 99.00, '含专属导游服务', 2, 1),
(1, '豪华套餐', 258.00, 129.00, '含全程专车接送', 3, 1);

-- 为行程ID=1 添加批次套餐
INSERT INTO `batch_package` (`tour_id`, `name`, `extra_fee_per_person`, `description`, `sort_order`, `status`) VALUES
(1, '工贸集散·标准套餐', 0.00, '在工贸集合点集合', 1, 1),
(1, '工贸集散·含餐套餐', 30.00, '含午餐', 2, 1),
(1, '市区上门接送', 50.00, '含主城区上门接送服务', 3, 1);

-- 为行程ID=1 添加出发日期
INSERT INTO `tour_batch` (`tour_id`, `departure_date`, `adult_date_extra_fee`, `child_date_extra_fee`, `status`, `remaining`, `max_capacity`) VALUES
(1, '2026-05-05', 0.00, 0.00, '可报名', 28, 30),
(1, '2026-05-10', 0.00, 0.00, '可报名', 15, 30),
(1, '2026-05-12', 0.00, 0.00, '可报名', 30, 30),
(1, '2026-05-17', 0.00, 0.00, '可报名', 22, 30),
(1, '2026-05-21', 0.00, 0.00, '可报名', 30, 30),
(1, '2026-06-01', 0.00, 0.00, '可报名', 45, 50);

-- 为行程ID=2 (南海邮轮) 添加套餐
INSERT INTO `tour_package` (`tour_id`, `name`, `adult_price`, `child_price`, `description`, `sort_order`, `status`) VALUES
(2, '内舱房', 4680.00, 2340.00, '标准内舱', 1, 1),
(2, '海景房', 5680.00, 2840.00, '海景阳台', 2, 1),
(2, '豪华套房', 7680.00, 3840.00, '豪华海景套房', 3, 1);

INSERT INTO `batch_package` (`tour_id`, `name`, `extra_fee_per_person`, `description`, `sort_order`, `status`) VALUES
(2, '标准出发', 0.00, '三亚码头集合', 1, 1),
(2, '含接送', 200.00, '含市区接送服务', 2, 1);

INSERT INTO `tour_batch` (`tour_id`, `departure_date`, `adult_date_extra_fee`, `child_date_extra_fee`, `status`, `remaining`, `max_capacity`) VALUES
(2, '2026-05-01', 0.00, 0.00, '可报名', 120, 200),
(2, '2026-05-13', 0.00, 0.00, '可报名', 80, 200),
(2, '2026-05-21', 200.00, 100.00, '可报名', 150, 200);

-- 为行程ID=3 (峨眉山) 添加套餐
INSERT INTO `tour_package` (`tour_id`, `name`, `adult_price`, `child_price`, `description`, `sort_order`, `status`) VALUES
(3, '标准团', 1280.00, 640.00, '标准住宿', 1, 1),
(3, '品质团', 1680.00, 840.00, '四星酒店', 2, 1),
(3, '豪华团', 2280.00, 1140.00, '五星酒店', 3, 1);

INSERT INTO `batch_package` (`tour_id`, `name`, `extra_fee_per_person`, `description`, `sort_order`, `status`) VALUES
(3, '成都集散', 0.00, '成都集合', 1, 1),
(3, '含接机', 100.00, '含双流机场接机', 2, 1);

INSERT INTO `tour_batch` (`tour_id`, `departure_date`, `adult_date_extra_fee`, `child_date_extra_fee`, `status`, `remaining`, `max_capacity`) VALUES
(3, '2026-05-15', 0.00, 0.00, '可报名', 25, 30),
(3, '2026-05-22', 0.00, 0.00, '可报名', 30, 30),
(3, '2026-05-29', 0.00, 0.00, '可报名', 18, 30);
