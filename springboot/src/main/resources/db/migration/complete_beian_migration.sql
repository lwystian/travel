-- ============================================
-- 公安备案合规 - 数据库迁移脚本
-- 执行前请备份数据库
-- ============================================

-- 1. 用户表添加登录日志相关字段
-- 注意：需要手动执行以下语句，如果字段已存在会报错
-- ALTER TABLE user ADD COLUMN register_ip VARCHAR(50) DEFAULT NULL COMMENT '注册IP';
-- ALTER TABLE user ADD COLUMN register_port INT DEFAULT NULL COMMENT '注册端口';
-- ALTER TABLE user ADD COLUMN last_login_ip VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP';
-- ALTER TABLE user ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间';
-- ALTER TABLE user ADD COLUMN login_count INT DEFAULT 0 COMMENT '登录次数';

-- 2. 创建系统操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `operation_type` VARCHAR(20) DEFAULT NULL COMMENT '操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/QUERY',
    `operation_desc` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
    `target_id` VARCHAR(50) DEFAULT NULL COMMENT '操作对象ID',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `port` INT DEFAULT NULL COMMENT '端口',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
    `status` INT DEFAULT 1 COMMENT '响应状态: 1-成功, 0-失败',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `execution_time` BIGINT DEFAULT NULL COMMENT '执行时长(毫秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_operation_type` (`operation_type`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_ip_address` (`ip_address`),
    INDEX `idx_target_type` (`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- 3. 评论表添加审核相关字段
-- ALTER TABLE `comment` 
--     ADD COLUMN `review_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-已通过, 2-已拒绝',
--     ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
--     ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人用户名',
--     ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
--     ADD COLUMN `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见';

-- 4. 攻略表添加审核相关字段
-- ALTER TABLE `travel_guide` 
--     ADD COLUMN `review_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-已通过, 2-已拒绝',
--     ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
--     ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人用户名',
--     ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
--     ADD COLUMN `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见';

-- ============================================
-- 请手动执行以下语句（逐条执行）
-- ============================================

-- ALTER TABLE user ADD COLUMN register_ip VARCHAR(50) DEFAULT NULL COMMENT '注册IP';
-- ALTER TABLE user ADD COLUMN register_port INT DEFAULT NULL COMMENT '注册端口';
-- ALTER TABLE user ADD COLUMN last_login_ip VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP';
-- ALTER TABLE user ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间';
-- ALTER TABLE user ADD COLUMN login_count INT DEFAULT 0 COMMENT '登录次数';

-- ALTER TABLE `comment` 
--     ADD COLUMN `review_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-已通过, 2-已拒绝',
--     ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
--     ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人用户名',
--     ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
--     ADD COLUMN `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见';

-- ALTER TABLE `travel_guide` 
--     ADD COLUMN `review_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-已通过, 2-已拒绝',
--     ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
--     ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人用户名',
--     ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
--     ADD COLUMN `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见';

-- ============================================
-- 迁移完成后，将现有数据设置为已审核
-- ============================================
-- UPDATE comment SET review_status = 1 WHERE review_status IS NULL OR review_status = 0;
-- UPDATE travel_guide SET review_status = 1 WHERE review_status IS NULL OR review_status = 0;
