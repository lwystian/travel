-- Clean initial schema for new Travel deployments.
-- Generated from the current local database structure only.
-- No historical users, orders, logs, test data, or production secrets are included.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `accommodation`;
DROP TABLE IF EXISTS `accommodation_review`;
DROP TABLE IF EXISTS `auth_provider_config`;
DROP TABLE IF EXISTS `backup_log`;
DROP TABLE IF EXISTS `batch_package`;
DROP TABLE IF EXISTS `behavior_log`;
DROP TABLE IF EXISTS `carousel`;
DROP TABLE IF EXISTS `collection`;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `comment_like`;
DROP TABLE IF EXISTS `frequent_traveler`;
DROP TABLE IF EXISTS `home_recommend`;
DROP TABLE IF EXISTS `login_log`;
DROP TABLE IF EXISTS `payment_config`;
DROP TABLE IF EXISTS `review_log`;
DROP TABLE IF EXISTS `scenic_category`;
DROP TABLE IF EXISTS `scenic_collection`;
DROP TABLE IF EXISTS `scenic_spot`;
DROP TABLE IF EXISTS `scenic_spot_tag`;
DROP TABLE IF EXISTS `scenic_tag`;
DROP TABLE IF EXISTS `sensitive_log`;
DROP TABLE IF EXISTS `sensitive_word`;
DROP TABLE IF EXISTS `site_notification`;
DROP TABLE IF EXISTS `sys_log`;
DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `tour`;
DROP TABLE IF EXISTS `tour_batch`;
DROP TABLE IF EXISTS `tour_collection`;
DROP TABLE IF EXISTS `tour_hotel`;
DROP TABLE IF EXISTS `tour_order`;
DROP TABLE IF EXISTS `tour_order_traveler`;
DROP TABLE IF EXISTS `tour_package`;
DROP TABLE IF EXISTS `travel_guide`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `accommodation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scenic_id` bigint DEFAULT NULL ,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `price_range` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `star_level` decimal(2,1) DEFAULT NULL ,
  `image_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `features` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `distance` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `scenic_id` (`scenic_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `accommodation_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `accommodation_id` bigint DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `rating` decimal(2,1) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `review_status` tinyint DEFAULT '0' ,
  `reviewer_id` bigint DEFAULT NULL ,
  `reviewer_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `review_time` datetime DEFAULT NULL ,
  `review_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL ,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `accommodation_id` (`accommodation_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `auth_provider_config` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `config_type` varchar(40) NOT NULL ,
  `config_name` varchar(80) NOT NULL ,
  `enabled` tinyint(1) NOT NULL DEFAULT '0' ,
  `config_data` text ,
  `description` varchar(255) DEFAULT NULL ,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_provider_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `backup_log` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `operator_id` bigint NOT NULL ,
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `backup_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `backup_table` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `backup_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `backup_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `backup_size` bigint DEFAULT NULL ,
  `backup_condition` text COLLATE utf8mb4_unicode_ci ,
  `record_count` int DEFAULT NULL ,
  `time_range_start` datetime DEFAULT NULL ,
  `time_range_end` datetime DEFAULT NULL ,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS' ,
  `error_msg` text COLLATE utf8mb4_unicode_ci ,
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `expire_time` datetime DEFAULT NULL ,
  `status_flag` tinyint DEFAULT '1' ,
  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_backup_type` (`backup_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `batch_package` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `tour_id` bigint NOT NULL ,
  `name` varchar(100) NOT NULL ,
  `extra_fee_per_person` decimal(10,2) DEFAULT '0.00' ,
  `description` varchar(500) DEFAULT '' ,
  `sort_order` int DEFAULT '0' ,
  `status` int DEFAULT '1' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  KEY `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `behavior_log` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint DEFAULT NULL ,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `behavior_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `behavior_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `page_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `page_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `element_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `element_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `referrer` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `stay_duration` int DEFAULT NULL ,
  `action_data` text COLLATE utf8mb4_unicode_ci ,
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `device_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `browser` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `os` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `screen_size` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `status` tinyint DEFAULT '1' ,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_behavior_type` (`behavior_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `carousel` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `image_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
  `status` tinyint DEFAULT '1' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `collection` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint DEFAULT NULL ,
  `guide_id` bigint DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `guide_id` (`guide_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint DEFAULT NULL ,
  `scenic_id` bigint DEFAULT NULL ,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ,
  `rating` int DEFAULT NULL ,
  `likes` int DEFAULT '0' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `review_status` tinyint DEFAULT '0',
  `reviewer_id` bigint DEFAULT NULL,
  `reviewer_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `review_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `scenic_id` (`scenic_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint DEFAULT NULL ,
  `comment_id` bigint DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`) USING BTREE,
  KEY `comment_id` (`comment_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `frequent_traveler` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(50) NOT NULL,
  `id_type` varchar(20) DEFAULT 'ID_CARD',
  `id_number` varchar(50) NOT NULL,
  `birth_date` date DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `traveler_type` varchar(20) DEFAULT 'ADULT',
  `phone` varchar(20) DEFAULT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `home_recommend` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `type` varchar(50) NOT NULL ,
  `tour_id` bigint NOT NULL ,
  `sort_order` int DEFAULT '0' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint DEFAULT NULL ,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `login_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' ,
  `login_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS' ,
  `fail_reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `port` int DEFAULT NULL ,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `device_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `browser` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `os` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `token` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `logout_time` datetime DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `status` tinyint DEFAULT '1' ,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_status` (`login_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payment_config` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `payment_type` varchar(50) NOT NULL ,
  `payment_name` varchar(100) NOT NULL ,
  `enabled` tinyint(1) NOT NULL DEFAULT '0' ,
  `config_data` text ,
  `gateway_url` varchar(500) DEFAULT NULL ,
  `is_sandbox` tinyint(1) NOT NULL DEFAULT '1' ,
  `icon` varchar(255) DEFAULT NULL ,
  `description` varchar(500) DEFAULT NULL ,
  `sort_order` int NOT NULL DEFAULT '0' ,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP ,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_type` (`payment_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review_log` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint NOT NULL ,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `target_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `target_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `target_user_id` bigint DEFAULT NULL ,
  `target_summary` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `content_preview` text COLLATE utf8mb4_unicode_ci ,
  `review_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `review_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `previous_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `review_tags` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `ai_score` decimal(5,4) DEFAULT NULL ,
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `status` tinyint DEFAULT '1' ,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_target_type` (`target_type`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_target_user_id` (`target_user_id`),
  KEY `idx_review_status` (`review_status`),
  KEY `idx_review_type` (`review_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `scenic_category` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `icon` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `parent_id` bigint DEFAULT '0' ,
  `sort_order` int DEFAULT '0' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `scenic_collection` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint NOT NULL ,
  `scenic_id` bigint NOT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_scenic` (`user_id`,`scenic_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_scenic_id` (`scenic_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `scenic_spot` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ,
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `price` decimal(10,2) DEFAULT NULL ,
  `opening_hours` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `image_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `longitude` decimal(10,6) DEFAULT NULL ,
  `latitude` decimal(10,6) DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  `category_id` bigint DEFAULT NULL ,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `scenic_spot_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `scenic_spot_id` bigint NOT NULL ,
  `tag_id` bigint NOT NULL ,
  PRIMARY KEY (`id`),
  KEY `idx_scenic_spot_id` (`scenic_spot_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `scenic_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `name` varchar(50) NOT NULL ,
  `color` varchar(20) DEFAULT '#FF6B35' ,
  `icon` varchar(50) DEFAULT '' ,
  `sort_order` int DEFAULT '0' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sensitive_log` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint DEFAULT NULL ,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `content` text COLLATE utf8mb4_unicode_ci ,
  `content_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `object_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `detected_words` text COLLATE utf8mb4_unicode_ci ,
  `detected_count` int DEFAULT '0' ,
  `handle_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `handle_result` text COLLATE utf8mb4_unicode_ci ,
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `status` tinyint DEFAULT '1' ,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_content_type` (`content_type`),
  KEY `idx_handle_type` (`handle_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sensitive_word` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `word` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `category` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER' ,
  `level` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REVIEW' ,
  `match_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CONTAINS' ,
  `replacement` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '***' ,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `status` tinyint NOT NULL DEFAULT '1' ,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sensitive_word` (`word`),
  KEY `idx_sensitive_word_category` (`category`),
  KEY `idx_sensitive_word_level` (`level`),
  KEY `idx_sensitive_word_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `site_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint NOT NULL ,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL ,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' ,
  `business_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `business_id` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `link_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `read_status` tinyint NOT NULL DEFAULT '0' ,
  `read_time` datetime DEFAULT NULL ,
  `sender_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' ,
  `sender_id` bigint DEFAULT NULL ,
  `sender_name` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `status` tinyint NOT NULL DEFAULT '1' ,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_read` (`user_id`,`read_status`),
  KEY `idx_notification_create_time` (`create_time`),
  KEY `idx_notification_business` (`business_type`,`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint DEFAULT NULL ,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `operation` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL ,
  `operation_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `object_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `object_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `object_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `method` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `request_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `request_params` text COLLATE utf8mb4_unicode_ci ,
  `request_body` text COLLATE utf8mb4_unicode_ci ,
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `port` int DEFAULT NULL ,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL ,
  `result` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS' ,
  `error_msg` text COLLATE utf8mb4_unicode_ci ,
  `response_data` text COLLATE utf8mb4_unicode_ci ,
  `duration` int DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `status` tinyint DEFAULT '1' ,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_operation` (`operation`),
  KEY `idx_object_type` (`object_type`),
  KEY `idx_object_id` (`object_id`),
  KEY `idx_result` (`result`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `operation_type` varchar(20) DEFAULT NULL,
  `log_level` varchar(20) DEFAULT 'INFO' ,
  `operation_desc` varchar(200) DEFAULT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `target_id` varchar(50) DEFAULT NULL,
  `request_url` varchar(500) DEFAULT NULL,
  `request_method` varchar(10) DEFAULT NULL,
  `request_params` text,
  `ip_address` varchar(50) DEFAULT NULL,
  `port` int DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `status` int DEFAULT '1',
  `error_message` text,
  `execution_time` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_ip_address` (`ip_address`),
  KEY `idx_log_level` (`log_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tour` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `code` varchar(50) DEFAULT '' ,
  `title` varchar(200) NOT NULL ,
  `subtitle` varchar(100) DEFAULT '' ,
  `main_image` varchar(500) DEFAULT '' ,
  `tag` varchar(100) DEFAULT '' ,
  `tour_type` varchar(20) DEFAULT '' ,
  `city` varchar(50) DEFAULT '' ,
  `destination` varchar(50) DEFAULT '' ,
  `days` int DEFAULT '1' ,
  `month` int DEFAULT '1' ,
  `min_price` decimal(10,2) DEFAULT '0.00' ,
  `star_rating` decimal(2,1) DEFAULT '5.0' ,
  `recommend_date` varchar(50) DEFAULT '' ,
  `more_dates` varchar(200) DEFAULT '' ,
  `feature` varchar(500) DEFAULT '' ,
  `tags` varchar(500) DEFAULT '' ,
  `enrolled_count` int DEFAULT '0' ,
  `theme` varchar(20) DEFAULT '' ,
  `status` int DEFAULT '1' ,
  `video_url` varchar(500) DEFAULT '' ,
  `video_poster` varchar(500) DEFAULT '' ,
  `video_enabled` int DEFAULT '0' ,
  `images` text ,
  `notice` varchar(500) DEFAULT '' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  KEY `idx_tour_type` (`tour_type`),
  KEY `idx_city` (`city`),
  KEY `idx_destination` (`destination`),
  KEY `idx_theme` (`theme`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tour_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `tour_id` bigint NOT NULL ,
  `departure_date` date NOT NULL ,
  `adult_date_extra_fee` decimal(10,2) DEFAULT '0.00' ,
  `child_date_extra_fee` decimal(10,2) DEFAULT '0.00' ,
  `status` varchar(20) DEFAULT '可报名' ,
  `remaining` int DEFAULT '0' ,
  `max_capacity` int DEFAULT '50' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  `locked` int DEFAULT '0' ,
  `occupied` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tour_date` (`tour_id`,`departure_date`),
  KEY `idx_departure_date` (`departure_date`),
  KEY `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tour_collection` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `user_id` bigint NOT NULL ,
  `tour_id` bigint NOT NULL ,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tour_collection_user_tour` (`user_id`,`tour_id`),
  KEY `idx_tour_collection_user` (`user_id`),
  KEY `idx_tour_collection_tour` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tour_hotel` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tour_id` bigint NOT NULL ,
  `accommodation_id` bigint DEFAULT NULL ,
  `name` varchar(200) NOT NULL ,
  `type` varchar(50) DEFAULT NULL ,
  `price_per_night` decimal(10,2) DEFAULT '0.00' ,
  `days` int DEFAULT '1' ,
  `enabled` tinyint DEFAULT '1' ,
  `image_url` varchar(500) DEFAULT NULL ,
  `star_level` decimal(3,1) DEFAULT NULL ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tour_order` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `order_no` varchar(50) NOT NULL ,
  `user_id` bigint NOT NULL ,
  `tour_id` bigint NOT NULL ,
  `tour_name` varchar(255) NOT NULL ,
  `tour_code` varchar(50) NOT NULL ,
  `package_id` bigint NOT NULL ,
  `package_name` varchar(100) NOT NULL ,
  `batch_package_id` bigint DEFAULT NULL ,
  `batch_package_name` varchar(100) DEFAULT '标准' ,
  `departure_date` date NOT NULL ,
  `adult_count` int NOT NULL DEFAULT '1' ,
  `child_count` int DEFAULT '0' ,
  `adult_unit_price` decimal(10,2) NOT NULL ,
  `child_unit_price` decimal(10,2) DEFAULT '0.00' ,
  `tour_amount` decimal(10,2) NOT NULL ,
  `hotel_id` bigint DEFAULT NULL ,
  `hotel_name` varchar(255) DEFAULT NULL ,
  `hotel_days` int DEFAULT NULL ,
  `hotel_price_per_night` decimal(10,2) DEFAULT NULL ,
  `hotel_amount` decimal(10,2) DEFAULT '0.00' ,
  `total_amount` decimal(10,2) NOT NULL ,
  `contact_name` varchar(50) DEFAULT NULL ,
  `contact_phone` varchar(20) DEFAULT NULL ,
  `status` int NOT NULL DEFAULT '0' ,
  `payment_method` varchar(50) DEFAULT NULL ,
  `payment_time` datetime DEFAULT NULL ,
  `remark` text ,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tour_id` (`tour_id`),
  KEY `idx_departure_date` (`departure_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tour_order_traveler` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `order_id` bigint NOT NULL ,
  `order_no` varchar(50) NOT NULL ,
  `name` varchar(50) NOT NULL ,
  `id_type` varchar(20) NOT NULL ,
  `id_number` varchar(50) NOT NULL ,
  `birth_date` date DEFAULT NULL ,
  `gender` varchar(10) NOT NULL ,
  `traveler_type` varchar(20) NOT NULL ,
  `phone` varchar(20) DEFAULT NULL ,
  `traveler_index` int DEFAULT '1' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tour_package` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `tour_id` bigint NOT NULL ,
  `name` varchar(100) NOT NULL ,
  `adult_price` decimal(10,2) NOT NULL DEFAULT '0.00' ,
  `child_price` decimal(10,2) DEFAULT '0.00' ,
  `description` varchar(500) DEFAULT '' ,
  `sort_order` int DEFAULT '0' ,
  `status` int DEFAULT '1' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`id`),
  KEY `idx_tour_id` (`tour_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `travel_guide` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ,
  `user_id` bigint DEFAULT NULL ,
  `cover_image` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `views` int DEFAULT '0' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  `destination` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' ,
  `review_status` tinyint DEFAULT '0',
  `reviewer_id` bigint DEFAULT NULL,
  `reviewer_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  `review_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT ,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `email` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'USER' ,
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `status` tinyint DEFAULT '1' ,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `register_ip` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `register_port` int DEFAULT NULL ,
  `last_login_ip` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL ,
  `last_login_time` datetime DEFAULT NULL ,
  `login_count` int DEFAULT '0' ,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  UNIQUE KEY `uk_email` (`email`) USING BTREE,
  UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Foreign keys
-- ----------------------------

ALTER TABLE `accommodation` ADD CONSTRAINT `accommodation_ibfk_1` FOREIGN KEY (`scenic_id`) REFERENCES `scenic_spot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `accommodation_review` ADD CONSTRAINT `accommodation_review_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `accommodation_review` ADD CONSTRAINT `accommodation_review_ibfk_2` FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `collection` ADD CONSTRAINT `collection_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `collection` ADD CONSTRAINT `collection_ibfk_2` FOREIGN KEY (`guide_id`) REFERENCES `travel_guide` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `comment` ADD CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `comment` ADD CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`scenic_id`) REFERENCES `scenic_spot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `comment_like` ADD CONSTRAINT `comment_like_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `comment_like` ADD CONSTRAINT `comment_like_ibfk_2` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `scenic_collection` ADD CONSTRAINT `scenic_collection_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `scenic_collection` ADD CONSTRAINT `scenic_collection_ibfk_2` FOREIGN KEY (`scenic_id`) REFERENCES `scenic_spot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `travel_guide` ADD CONSTRAINT `travel_guide_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- ----------------------------
-- Clean seed data
-- ----------------------------

INSERT INTO `auth_provider_config`
(`config_type`, `config_name`, `enabled`, `config_data`, `description`)
VALUES
('aliyun_sms', '阿里云短信', 0, '{"enabled":false,"configured":false,"accessKeyId":"","accessKeySecret":"","signName":"","templateCode":"","orderUserTemplateCode":"","orderAdminTemplateCode":"","regionId":"cn-hangzhou","endpoint":"dysmsapi.aliyuncs.com","codeExpireMinutes":5,"sendIntervalSeconds":60,"dailyLimit":5}', '用于登录注册、订单通知等短信验证码发送'),
('geetest', '极验验证', 0, '{"enabled":false,"configured":false,"captchaId":"","captchaKey":""}', '用于短信发送前的人机验证'),
('email_smtp', 'SMTP邮件', 0, '{"enabled":false,"configured":false,"host":"","port":465,"username":"","password":"","fromEmail":"","protocol":"smtp","sslEnabled":true,"codeExpireMinutes":10,"sendIntervalSeconds":60}', '用于邮箱绑定和邮件验证码发送'),
('site_access', '网站访问控制', 1, '{"siteEnabled":true,"closedTitle":"网站维护中","closedMessage":"我们正在进行系统维护与服务升级，完成后将恢复访问。","closedContact":"如有紧急订单或出行问题，请联系官方客服处理。","rejectMobile":false,"mobileTitle":"请使用电脑访问","mobileMessage":"当前移动端体验正在优化，如遇显示异常可使用电脑访问。","mobileContact":"如需咨询行程，可通过官方客服渠道联系我们。","supportButtonText":"联系官方客服","supportUrl":"","supportCredential":"","supportQrImageUrl":""}', '控制官网前台开放状态和移动端访问策略'),
('site_assets', '站点图片配置', 1, '{"logoUrl":"","wechatQrUrl":"","authBackgroundUrl":"","aboutHeroUrl":"","legalHeroUrl":"","accommodationHeroUrl":"","placeholderImageUrl":""}', '用于前台模板的可配置图片'),
('site_footer', '网站页脚配置', 1, '{"enabled":true,"brandName":"旅行社官网","companyName":"请在后台填写公司名称","slogan":"提供可靠、清晰、可追踪的旅行服务","consultationPhone":"","cruisePhone":"","serviceTime":"09:00 - 18:00","address":"","icpNumber":"","icpUrl":"https://beian.miit.gov.cn","policeNumber":"","policeUrl":"https://beian.mps.gov.cn","licenseNumber":"","complaintPhone":"","technicalSupport":"","reportEmail":"","minorReportEmail":"","copyright":"© 2026 旅行社官网 版权所有","topLinks":[{"label":"关于我们","url":"/about"},{"label":"旅游线路","url":"/tickets"},{"label":"目的地","url":"/scenic"},{"label":"旅行攻略","url":"/guide"}],"friendlyLinks":[],"qrCodes":[],"certificates":[],"complianceLinks":[{"label":"营业执照","url":"/legal/business-license"},{"label":"旅行社业务经营许可","url":"/legal/travel-license"},{"label":"服务规范","url":"/legal/service-standard"},{"label":"社区公约","url":"/legal/community-guidelines"},{"label":"安全与隐私保护","url":"/legal/privacy-safety"},{"label":"在线服务与投诉反馈专区","url":"/legal/support-feedback"}],"legalPages":[],"legalNotes":[]}', '用于网站页脚、备案、二维码、证书和合规页面展示'),
('page_content:home', '页面内容配置-home', 1, '{"sections":{},"trustMetrics":[],"capabilityList":[],"scenarioList":[],"assuranceSteps":[],"processSteps":[]}', '首页可编辑文案配置'),
('page_content:about', '页面内容配置-about', 1, '{"pageContent":{}}', '关于我们页面可编辑文案配置'),
('page_content:frontend-header', '页面内容配置-frontend-header', 1, '{"welcomeText":"欢迎访问旅行社官网","brandName":"旅行社官网","brandSubtitle":"品质旅行服务","wechatQrText":"扫码关注","logoLinkUrl":"/"}', '前台头部可编辑文案配置');

INSERT INTO `payment_config`
(`payment_type`, `payment_name`, `enabled`, `config_data`, `gateway_url`, `is_sandbox`, `icon`, `description`, `sort_order`)
VALUES
('alipay', '支付宝', 0, '{"paymentType":"alipay","appId":"","privateKey":"","alipayPublicKey":"","isSandbox":true,"gatewayUrl":"https://openapi-sandbox.dl.alipaydev.com/gateway.do","timeoutExpress":"2h"}', 'https://openapi-sandbox.dl.alipaydev.com/gateway.do', 1, 'alipay', '支付宝网页支付，请在后台填写正式配置后启用', 1);

INSERT INTO `scenic_category`
(`id`, `name`, `description`, `icon`, `parent_id`, `sort_order`, `status`, `create_time`, `update_time`)
VALUES
(1, '自然风光', '山水、湖泊、瀑布、海岛等自然景观', '', 0, 1, 1, NOW(), NOW()),
(2, '历史文化', '古迹、博物馆、历史遗址、文化街区等', '', 0, 2, 1, NOW(), NOW()),
(3, '主题乐园', '主题乐园、亲子游乐、演艺体验等', '', 0, 3, 1, NOW(), NOW()),
(4, '城市地标', '城市建筑、观景平台、热门街区等', '', 0, 4, 1, NOW(), NOW()),
(5, '乡村民俗', '乡村风光、民俗文化、农旅体验等', '', 0, 5, 1, NOW(), NOW());

INSERT INTO `scenic_tag`
(`id`, `name`, `color`, `description`, `status`, `sort_order`, `create_time`, `update_time`)
VALUES
(1, '自然风光', '#22c55e', '自然景观类标签', 1, 1, NOW(), NOW()),
(2, '历史文化', '#8b5cf6', '历史文化类标签', 1, 2, NOW(), NOW()),
(3, '亲子友好', '#f59e0b', '适合亲子家庭出行', 1, 3, NOW(), NOW()),
(4, '摄影推荐', '#0ea5e9', '适合拍照和摄影创作', 1, 4, NOW(), NOW()),
(5, '轻松休闲', '#14b8a6', '行程节奏轻松', 1, 5, NOW(), NOW());

INSERT INTO `sensitive_word`
(`word`, `category`, `level`, `match_type`, `replacement`, `remark`, `status`, `create_time`, `update_time`)
VALUES
('色情服务', 'PORN', 'BLOCK', 'CONTAINS', '***', '涉黄违法内容', 1, NOW(), NOW()),
('赌博', 'GAMBLING', 'BLOCK', 'CONTAINS', '***', '赌博违法内容', 1, NOW(), NOW()),
('博彩', 'GAMBLING', 'BLOCK', 'CONTAINS', '***', '赌博引流内容', 1, NOW(), NOW()),
('毒品', 'DRUG', 'BLOCK', 'CONTAINS', '***', '涉毒违法内容', 1, NOW(), NOW()),
('枪支', 'WEAPON', 'BLOCK', 'CONTAINS', '***', '涉枪违法内容', 1, NOW(), NOW()),
('爆炸物', 'WEAPON', 'BLOCK', 'CONTAINS', '***', '涉爆违法内容', 1, NOW(), NOW()),
('诈骗', 'FRAUD', 'REVIEW', 'CONTAINS', '***', '欺诈风险内容', 1, NOW(), NOW()),
('刷单', 'FRAUD', 'REVIEW', 'CONTAINS', '***', '灰产引流内容', 1, NOW(), NOW()),
('代开发票', 'FRAUD', 'BLOCK', 'CONTAINS', '***', '发票违法内容', 1, NOW(), NOW()),
('站外交易', 'FRAUD', 'REVIEW', 'CONTAINS', '***', '绕开平台交易', 1, NOW(), NOW()),
('加微信', 'CONTACT', 'REVIEW', 'CONTAINS', '***', '站外引流联系方式', 1, NOW(), NOW()),
('加QQ', 'CONTACT', 'REVIEW', 'CONTAINS', '***', '站外引流联系方式', 1, NOW(), NOW()),
('私下转账', 'CONTACT', 'REVIEW', 'CONTAINS', '***', '绕开平台交易', 1, NOW(), NOW()),
('政治谣言', 'POLITICAL', 'BLOCK', 'CONTAINS', '***', '政治谣言风险', 1, NOW(), NOW()),
('邪教', 'ILLEGAL', 'BLOCK', 'CONTAINS', '***', '非法组织相关内容', 1, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;
