ALTER TABLE `scenic_spot`
  ADD COLUMN `tags` varchar(500) DEFAULT NULL COMMENT '标签，多个标签用英文逗号分隔' AFTER `latitude`;

DROP TABLE IF EXISTS `scenic_spot_tag`;
DROP TABLE IF EXISTS `scenic_tag`;
