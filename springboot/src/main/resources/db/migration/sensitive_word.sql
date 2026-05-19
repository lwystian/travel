CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `word` VARCHAR(120) NOT NULL COMMENT '敏感词',
    `category` VARCHAR(40) NOT NULL DEFAULT 'OTHER' COMMENT '分类',
    `level` VARCHAR(20) NOT NULL DEFAULT 'REVIEW' COMMENT '处置级别: BLOCK/REVIEW/REPLACE',
    `match_type` VARCHAR(20) NOT NULL DEFAULT 'CONTAINS' COMMENT '匹配方式: CONTAINS/EXACT',
    `replacement` VARCHAR(40) NOT NULL DEFAULT '***' COMMENT '替换文本',
    `remark` VARCHAR(255) COMMENT '备注',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensitive_word` (`word`),
    INDEX `idx_sensitive_word_category` (`category`),
    INDEX `idx_sensitive_word_level` (`level`),
    INDEX `idx_sensitive_word_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词规则表';
