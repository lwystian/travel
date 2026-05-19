-- 系统操作日志表
-- 用于满足公安备案要求的操作审计日志留存（需保存至少6个月）

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `operation_type` VARCHAR(20) DEFAULT NULL COMMENT '操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/QUERY',
    `log_level` VARCHAR(20) DEFAULT 'INFO' COMMENT '日志等级: INFO/WARN/ERROR',
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
    INDEX `idx_log_level` (`log_level`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_ip_address` (`ip_address`),
    INDEX `idx_target_type` (`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';
