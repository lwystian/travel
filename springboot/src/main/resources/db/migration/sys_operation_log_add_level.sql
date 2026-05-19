ALTER TABLE `sys_operation_log`
    ADD COLUMN `log_level` VARCHAR(20) DEFAULT 'INFO' COMMENT '日志等级: INFO/WARN/ERROR' AFTER `operation_type`;

UPDATE `sys_operation_log`
SET `log_level` = CASE
    WHEN `error_message` IS NOT NULL AND `error_message` <> '' THEN 'ERROR'
    WHEN `status` = 0 THEN 'WARN'
    ELSE 'INFO'
END
WHERE `log_level` IS NULL OR `log_level` = '';
