-- 为 tour_batch 表添加锁定库存字段
-- 用于记录已下单但未付款占用的名额

ALTER TABLE tour_batch ADD COLUMN IF NOT EXISTS occupied INT DEFAULT 0 COMMENT '锁定库存（已下单未付款占用的名额）';

-- 初始化 occupied 字段为 0（如果已有数据）
UPDATE tour_batch SET occupied = 0 WHERE occupied IS NULL;
