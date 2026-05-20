-- 手机号注册登录适配：邮箱改为可选，手机号作为登录标识。
ALTER TABLE `user` MODIFY COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱';

-- 如果历史库还没有 phone 字段，再执行这一句；已有字段时不要重复执行。
-- ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `email`;

-- 如果已存在同名索引，或存在重复手机号，请先清理后再执行。
CREATE UNIQUE INDEX `uk_user_phone` ON `user` (`phone`);
