-- 为 travel_guide 表添加目的地字段
ALTER TABLE travel_guide ADD COLUMN destination VARCHAR(50) DEFAULT '' COMMENT '目的地代码';
