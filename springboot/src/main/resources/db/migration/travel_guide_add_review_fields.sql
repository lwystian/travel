-- 旅游攻略表添加审核相关字段
-- 用于满足公安备案要求的"先审后发"机制

ALTER TABLE `travel_guide` 
    ADD COLUMN `review_status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-已通过, 2-已拒绝' AFTER `views`,
    ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID' AFTER `review_status`,
    ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人用户名' AFTER `reviewer_id`,
    ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT '审核时间' AFTER `reviewer_name`,
    ADD COLUMN `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见' AFTER `review_time`;
