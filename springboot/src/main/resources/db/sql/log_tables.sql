-- ============================================
-- 系统日志审计表 - 完整建表脚本
-- 留存期限：至少6个月
-- 删除受控：需后台管理员操作
-- ============================================

-- 1. 操作日志表 - 记录用户的所有操作行为
CREATE TABLE IF NOT EXISTS `sys_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `nickname` VARCHAR(50) COMMENT '用户昵称',
    `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `operation_name` VARCHAR(100) COMMENT '操作名称',
    `object_type` VARCHAR(50) COMMENT '操作对象类型',
    `object_id` VARCHAR(100) COMMENT '操作对象ID',
    `object_name` VARCHAR(200) COMMENT '操作对象名称',
    `method` VARCHAR(200) COMMENT '请求方法',
    `request_url` VARCHAR(500) COMMENT '请求URL',
    `request_params` TEXT COMMENT '请求参数',
    `request_body` TEXT COMMENT '请求体',
    `ip` VARCHAR(50) COMMENT 'IP地址',
    `port` INT COMMENT '端口',
    `user_agent` VARCHAR(500) COMMENT 'User-Agent',
    `result` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '操作结果: SUCCESS/FAIL/ERROR',
    `error_msg` TEXT COMMENT '错误信息',
    `response_data` TEXT COMMENT '响应数据(脱敏)',
    `duration` INT COMMENT '操作耗时(毫秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_operation` (`operation`),
    INDEX `idx_object_type` (`object_type`),
    INDEX `idx_object_id` (`object_id`),
    INDEX `idx_result` (`result`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- 2. 登录日志表 - 记录用户登录登出行为
CREATE TABLE IF NOT EXISTS `login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `nickname` VARCHAR(50) COMMENT '用户昵称',
    `login_type` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '登录类型: NORMAL-正常登录 TOKEN-refresh_token登录 WECHAT-微信登录',
    `login_status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '登录状态: SUCCESS-成功 FAIL-失败',
    `fail_reason` VARCHAR(200) COMMENT '失败原因',
    `ip` VARCHAR(50) COMMENT 'IP地址',
    `port` INT COMMENT '端口',
    `user_agent` VARCHAR(500) COMMENT 'User-Agent',
    `device_type` VARCHAR(50) COMMENT '设备类型',
    `browser` VARCHAR(100) COMMENT '浏览器',
    `os` VARCHAR(100) COMMENT '操作系统',
    `location` VARCHAR(200) COMMENT '登录地点',
    `session_id` VARCHAR(100) COMMENT '会话ID',
    `token` VARCHAR(500) COMMENT 'Token(脱敏)',
    `logout_time` DATETIME COMMENT '登出时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_login_status` (`login_status`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 3. 敏感词日志表 - 记录敏感词检测和处理
CREATE TABLE IF NOT EXISTS `sensitive_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `content` TEXT COMMENT '检测内容',
    `content_type` VARCHAR(20) COMMENT '内容类型: COMMENT-评论 GUIDE-游记 DESCRIPTION-描述等',
    `object_id` VARCHAR(100) COMMENT '关联对象ID',
    `detected_words` TEXT COMMENT '检测到的敏感词(JSON数组)',
    `detected_count` INT DEFAULT 0 COMMENT '敏感词数量',
    `handle_type` VARCHAR(20) COMMENT '处理方式: REJECT-拒绝 REPLACE-替换 WARN-警告 PASS-放行',
    `handle_result` TEXT COMMENT '处理结果(替换后的内容等)',
    `ip` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT 'User-Agent',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_content_type` (`content_type`),
    INDEX `idx_handle_type` (`handle_type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词检测日志表';

-- 4. 审核日志表 - 记录内容审核操作
CREATE TABLE IF NOT EXISTS `review_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID(审核人)',
    `username` VARCHAR(50) NOT NULL COMMENT '操作用户名',
    `nickname` VARCHAR(50) COMMENT '操作人昵称',
    `target_type` VARCHAR(30) NOT NULL COMMENT '审核对象类型: COMMENT-评论 GUIDE-游记 IMAGE-图片 USER-用户',
    `target_id` VARCHAR(100) NOT NULL COMMENT '审核对象ID',
    `target_user_id` BIGINT COMMENT '被审核内容的用户ID',
    `target_summary` VARCHAR(200) COMMENT '被审核内容摘要',
    `content_preview` TEXT COMMENT '内容预览(前500字符)',
    `review_type` VARCHAR(20) NOT NULL COMMENT '审核类型: AUTO-自动 MANUAL-人工',
    `review_status` VARCHAR(20) NOT NULL COMMENT '审核结果: PENDING-待审核 APPROVED-通过 REJECTED-拒绝',
    `previous_status` VARCHAR(20) COMMENT '原状态',
    `reject_reason` VARCHAR(500) COMMENT '拒绝原因',
    `review_tags` VARCHAR(200) COMMENT '审核标签(JSON数组): 涉黄/涉暴/广告等',
    `ai_score` DECIMAL(5,4) COMMENT 'AI审核分数(0-1)',
    `ip` VARCHAR(50) COMMENT '审核人IP',
    `user_agent` VARCHAR(500) COMMENT 'User-Agent',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_target_type` (`target_type`),
    INDEX `idx_target_id` (`target_id`),
    INDEX `idx_target_user_id` (`target_user_id`),
    INDEX `idx_review_status` (`review_status`),
    INDEX `idx_review_type` (`review_type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核日志表';

-- 5. 数据备份记录表 - 记录备份操作
CREATE TABLE IF NOT EXISTS `backup_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '备份ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    `backup_type` VARCHAR(30) NOT NULL COMMENT '备份类型: LOG-日志 DB-数据库 FILE-文件',
    `backup_table` VARCHAR(50) COMMENT '备份的表名',
    `backup_name` VARCHAR(200) COMMENT '备份文件名称',
    `backup_path` VARCHAR(500) COMMENT '备份文件路径',
    `backup_size` BIGINT COMMENT '备份文件大小(字节)',
    `backup_condition` TEXT COMMENT '备份条件(Where子句)',
    `record_count` INT COMMENT '备份记录数',
    `time_range_start` DATETIME COMMENT '备份时间范围开始',
    `time_range_end` DATETIME COMMENT '备份时间范围结束',
    `status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '备份状态: SUCCESS-成功 FAIL-失败 RUNNING-进行中',
    `error_msg` TEXT COMMENT '错误信息',
    `file_url` VARCHAR(500) COMMENT '下载URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
    `expire_time` DATETIME COMMENT '过期时间(默认6个月后)',
    `status_flag` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    PRIMARY KEY (`id`),
    INDEX `idx_operator_id` (`operator_id`),
    INDEX `idx_backup_type` (`backup_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据备份记录表';

-- 6. 行为日志表 - 记录用户行为轨迹
CREATE TABLE IF NOT EXISTS `behavior_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `session_id` VARCHAR(100) COMMENT '会话ID',
    `behavior_type` VARCHAR(50) NOT NULL COMMENT '行为类型',
    `behavior_name` VARCHAR(100) COMMENT '行为名称',
    `page_url` VARCHAR(500) COMMENT '页面URL',
    `page_name` VARCHAR(100) COMMENT '页面名称',
    `element_id` VARCHAR(100) COMMENT '触发元素ID',
    `element_name` VARCHAR(100) COMMENT '触发元素名称',
    `referrer` VARCHAR(500) COMMENT '来源页面',
    `stay_duration` INT COMMENT '页面停留时长(秒)',
    `action_data` TEXT COMMENT '行为数据(JSON)',
    `ip` VARCHAR(50) COMMENT 'IP地址',
    `device_type` VARCHAR(50) COMMENT '设备类型',
    `browser` VARCHAR(100) COMMENT '浏览器',
    `os` VARCHAR(100) COMMENT '操作系统',
    `screen_size` VARCHAR(20) COMMENT '屏幕分辨率',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_behavior_type` (`behavior_type`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为日志表';

-- ============================================
-- 日志保留策略（建议通过定时任务执行）
-- 保留期限：6个月
-- 备份后删除，或直接归档
-- ============================================

-- 示例：清理6个月前的日志（执行前建议先备份）
-- DELETE FROM sys_log WHERE create_time < DATE_SUB(NOW(), INTERVAL 6 MONTH) AND status = 1;
-- DELETE FROM login_log WHERE create_time < DATE_SUB(NOW(), INTERVAL 6 MONTH) AND status = 1;
-- DELETE FROM sensitive_log WHERE create_time < DATE_SUB(NOW(), INTERVAL 6 MONTH) AND status = 1;
-- DELETE FROM review_log WHERE create_time < DATE_SUB(NOW(), INTERVAL 6 MONTH) AND status = 1;
-- DELETE FROM behavior_log WHERE create_time < DATE_SUB(NOW(), INTERVAL 6 MONTH) AND status = 1;
