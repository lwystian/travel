-- 用户表添加登录日志相关字段
-- 用于满足公安备案要求的用户注册IP、登录IP留存

ALTER TABLE user ADD COLUMN register_ip VARCHAR(50) DEFAULT NULL COMMENT '注册IP' AFTER update_time;
ALTER TABLE user ADD COLUMN register_port INT DEFAULT NULL COMMENT '注册端口' AFTER register_ip;
ALTER TABLE user ADD COLUMN last_login_ip VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP' AFTER register_port;
ALTER TABLE user ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER last_login_ip;
ALTER TABLE user ADD COLUMN login_count INT DEFAULT 0 COMMENT '登录次数' AFTER last_login_time;
