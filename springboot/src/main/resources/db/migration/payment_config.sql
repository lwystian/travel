-- 支付配置表
CREATE TABLE IF NOT EXISTS `payment_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `payment_type` VARCHAR(50) NOT NULL COMMENT '支付类型：alipay, wechat, unionpay等',
    `payment_name` VARCHAR(100) NOT NULL COMMENT '支付名称：支付宝、微信支付等',
    `enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    `config_data` TEXT COMMENT '配置数据（JSON格式，存储敏感信息）',
    `gateway_url` VARCHAR(500) COMMENT '网关地址',
    `is_sandbox` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否沙箱环境',
    `icon` VARCHAR(255) COMMENT '图标样式或URL',
    `description` VARCHAR(500) COMMENT '描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_type` (`payment_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付配置表';

-- 初始化默认支付宝配置
INSERT INTO `payment_config` (`payment_type`, `payment_name`, `enabled`, `is_sandbox`, `gateway_url`, `icon`, `description`, `sort_order`) 
VALUES ('alipay', '支付宝', 0, 1, 'https://openapi.alipay.com/gateway.do', 'alipay', '支付宝网页支付', 1)
ON DUPLICATE KEY UPDATE `payment_name` = VALUES(`payment_name`);
