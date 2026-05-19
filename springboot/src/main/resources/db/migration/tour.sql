-- 创建行程表
CREATE TABLE `tour` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行程ID',
  `code` VARCHAR(50) DEFAULT '' COMMENT '行程编号（业务编码）：周边游ZW, 长线游CX, 跟团游GT, 邮轮YL',
  `title` VARCHAR(200) NOT NULL COMMENT '行程名称',
  `subtitle` VARCHAR(100) DEFAULT '' COMMENT '副标题',
  `main_image` VARCHAR(500) DEFAULT '' COMMENT '封面图片URL',
  `tag` VARCHAR(100) DEFAULT '' COMMENT '标签',
  `tour_type` VARCHAR(20) DEFAULT '' COMMENT '行程类型: around-周边游, long-长线游, team-跟团游, cruise-邮轮出行',
  `city` VARCHAR(50) DEFAULT '' COMMENT '出发城市代码',
  `destination` VARCHAR(50) DEFAULT '' COMMENT '目的地代码',
  `days` INT DEFAULT 1 COMMENT '行程天数',
  `month` INT DEFAULT 1 COMMENT '出发月份',
  `min_price` DECIMAL(10,2) DEFAULT 0 COMMENT '最低价格',
  `star_rating` DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分',
  `recommend_date` VARCHAR(50) DEFAULT '' COMMENT '推荐日期',
  `more_dates` VARCHAR(200) DEFAULT '' COMMENT '更多日期',
  `feature` VARCHAR(500) DEFAULT '' COMMENT '行程特色',
  `tags` VARCHAR(500) DEFAULT '' COMMENT '标签列表(JSON数组格式)',
  `enrolled_count` INT DEFAULT 0 COMMENT '已报名人数',
  `theme` VARCHAR(20) DEFAULT '' COMMENT '主题: scenic-风景游, cultural-文化游, adventure-探险游, hiking-徒步游',
  `status` INT DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
  `video_url` VARCHAR(500) DEFAULT '' COMMENT '视频URL',
  `video_poster` VARCHAR(500) DEFAULT '' COMMENT '视频封面URL',
  `images` TEXT COMMENT '多张图片URL(JSON数组格式)',
  `notice` VARCHAR(500) DEFAULT '' COMMENT '出团通知',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tour_type` (`tour_type`),
  KEY `idx_city` (`city`),
  KEY `idx_destination` (`destination`),
  KEY `idx_theme` (`theme`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程表';

-- 插入示例行程数据
INSERT INTO `tour` (`title`, `subtitle`, `main_image`, `tag`, `tour_type`, `city`, `destination`, `days`, `month`, `min_price`, `star_rating`, `recommend_date`, `more_dates`, `feature`, `tags`, `enrolled_count`, `theme`, `status`) VALUES
-- 周边游
('南川风吹草原·风吹村·4天3晚轻徒步', '重庆出发', 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop', '周边游·轻徒步', 'around', 'chongqing', 'chongqing', 4, 5, 158, 4.9, '2026-05-05 周一', '05-10、05-12、05-17、05-21、06-01', '18km累计爬升1200米，龙岩城-小鹿池-石人峰，新人勿入！！！', '[\"强度穿越\", \"山城经典\", \"林海秘境\"]', 253, 'hiking', 1),

-- 西沙邮轮
('南海邮轮·南海之梦·三亚-三沙-三亚·3天2晚', '三亚出发', 'https://images.unsplash.com/photo-1548574505-5e239809ee19?w=400&h=300&fit=crop', '邮轮船票·西沙群岛航线', 'cruise', 'sanya', 'xisha', 3, 5, 4680, 4.8, '2026-05-01 周五', '05-13、05-21、05-25、05-29', '"五一特惠"预定南海之梦号邮轮5月1日航次，16岁以下青少年儿童入...', '[\"邮轮\", \"海岛\", \"西沙群岛\"]', 128, 'scenic', 1),
('海南海峡·祥龙岛·三亚-三沙-三亚·3天2晚', '三亚出发', 'https://images.unsplash.com/photo-1548574505-5e239809ee19?w=400&h=300&fit=crop', '邮轮船票·西沙群岛航线', 'cruise', 'sanya', 'xisha', 3, 5, 5490, 4.7, '2026-05-23 周六', '04-28、05-03、05-07、05-11、05-15、05-19、05-27', '官方代理【咨询客服享优惠】圆梦西沙 登赵述岛升国旗唱国歌 父母...', '[\"邮轮\", \"官方代理\", \"西沙\"]', 89, 'scenic', 1),

-- 跟团游
('峨眉山·乐山大佛·都江堰·青城山·4天3晚深度游', '成都出发·纯玩无购物', 'https://images.unsplash.com/photo-1554048612-b6a482bc67a4?w=400&h=300&fit=crop', '跟团游·文化古迹', 'team', 'chengdu', 'sichuan', 4, 5, 1280, 4.6, '2026-05-15 周四', '05-22、05-29、06-05', '深度游览四川四大景点，含全程星级住宿，金牌导游讲解', '[\"佛教圣地\", \"世界遗产\", \"纯玩\"]', 156, 'cultural', 1),
('稻城亚丁·色达·新都桥·7天6晚摄影之旅', '成都出发·摄影团·小团出行', 'https://images.unsplash.com/photo-1516483638261-f4dbaf03663a?w=400&h=300&fit=crop', '跟团游·摄影专线', 'team', 'chengdu', 'sichuan', 7, 6, 3280, 4.9, '2026-05-20 周三', '06-03、06-17、07-01', '摄影领队全程跟拍，独家机位，邂逅"蓝色星球上最后一片净土"', '[\"摄影天堂\", \"高原风光\", \"小团\"]', 67, 'scenic', 1),

-- 三峡邮轮
('长江三峡·世纪荣耀号·重庆-宜昌·3天2晚', '重庆朝天门码头登船', 'https://images.unsplash.com/photo-1537531383496-f4749b85ceb3?w=400&h=300&fit=crop', '邮轮船票·三峡航线', 'cruise', 'chongqing', 'sanxia', 3, 5, 1680, 4.8, '2026-05-15 周四', '05-22、05-29、06-05、06-12', '乘坐世纪荣耀号五星豪华游轮，畅游三峡精华段，含船上自助餐', '[\"豪华游轮\", \"三峡风光\", \"五星服务\"]', 312, 'scenic', 1),
('长江三峡·华夏神女号·重庆-宜昌·4天3晚', '重庆朝天门码头登船', 'https://images.unsplash.com/photo-1537531383496-f4749b85ceb3?w=400&h=300&fit=crop', '邮轮船票·三峡航线', 'cruise', 'chongqing', 'sanxia', 4, 6, 2280, 4.9, '2026-06-10 周二', '06-17、06-24、07-01', '华夏神女号主题游轮，含丰都鬼城、白帝城、瞿塘峡、巫峡、西陵峡', '[\"主题游轮\", \"三峡全景\", \"精华景点\"]', 256, 'scenic', 1),
('长江三峡·美维凯珍号·宜昌-重庆·5天4晚', '宜昌港登船', 'https://images.unsplash.com/photo-1537531383496-f4749b85ceb3?w=400&h=300&fit=crop', '邮轮船票·三峡航线', 'cruise', 'yichang', 'sanxia', 5, 7, 3180, 4.7, '2026-07-05 周六', '07-12、07-19、07-26', '逆流而上深度游三峡，含三峡大坝、升船机体验、神农溪漂流', '[\"逆流深度\", \"升船机\", \"神农溪\"]', 178, 'scenic', 1),
('西沙群岛·全富岛·银屿岛·4天3晚圆梦之旅', '三亚出发·南海之梦号', 'https://images.unsplash.com/photo-1548574505-5e239809ee19?w=400&h=300&fit=crop', '邮轮船票·西沙群岛航线', 'cruise', 'sanya', 'xisha', 4, 6, 5980, 4.9, '2026-06-01 周一', '06-08、06-15、06-22', '【西沙圆梦】登全富岛升国旗，银屿岛浮潜，体验西沙纯净海域', '[\"西沙群岛\", \"邮轮\", \"升旗仪式\"]', 234, 'scenic', 1);

