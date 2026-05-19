package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SensitiveWordSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitiveWordSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            createTable();
            seedDefaults();
        } catch (Exception e) {
            LOGGER.warn("初始化敏感词规则表失败，请检查数据库权限或手动执行初始化 SQL", e);
        }
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sensitive_word` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
                    `word` VARCHAR(120) NOT NULL COMMENT '敏感词',
                    `category` VARCHAR(40) NOT NULL DEFAULT 'OTHER' COMMENT '分类',
                    `level` VARCHAR(20) NOT NULL DEFAULT 'REVIEW' COMMENT '处置级别: BLOCK/REVIEW/REPLACE',
                    `match_type` VARCHAR(20) NOT NULL DEFAULT 'CONTAINS' COMMENT '匹配方式: CONTAINS/EXACT',
                    `replacement` VARCHAR(40) NOT NULL DEFAULT '***' COMMENT '替换文本',
                    `remark` VARCHAR(255) COMMENT '备注',
                    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uk_sensitive_word` (`word`),
                    INDEX `idx_sensitive_word_category` (`category`),
                    INDEX `idx_sensitive_word_level` (`level`),
                    INDEX `idx_sensitive_word_status` (`status`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词规则表'
                """);
    }

    private void seedDefaults() {
        Object[][] rows = new Object[][] {
                {"色情服务", "PORN", "BLOCK", "涉黄违法内容"},
                {"约炮", "PORN", "BLOCK", "涉黄引流内容"},
                {"裸聊", "PORN", "BLOCK", "涉黄引流内容"},
                {"成人视频", "PORN", "BLOCK", "涉黄违法内容"},
                {"偷拍视频", "PORN", "BLOCK", "侵犯隐私及涉黄风险"},
                {"赌博", "GAMBLING", "BLOCK", "赌博违法内容"},
                {"博彩", "GAMBLING", "BLOCK", "赌博引流内容"},
                {"网投", "GAMBLING", "BLOCK", "网络赌博引流"},
                {"私彩", "GAMBLING", "BLOCK", "赌博违法内容"},
                {"赌球", "GAMBLING", "BLOCK", "网络赌博内容"},
                {"毒品", "DRUG", "BLOCK", "涉毒违法内容"},
                {"冰毒", "DRUG", "BLOCK", "涉毒违法内容"},
                {"麻古", "DRUG", "BLOCK", "涉毒违法内容"},
                {"K粉", "DRUG", "BLOCK", "涉毒违法内容"},
                {"摇头丸", "DRUG", "BLOCK", "涉毒违法内容"},
                {"枪支", "WEAPON", "BLOCK", "涉枪违法内容"},
                {"买枪", "WEAPON", "BLOCK", "涉枪违法内容"},
                {"管制刀具", "WEAPON", "BLOCK", "危险品违法内容"},
                {"爆炸物", "WEAPON", "BLOCK", "涉爆违法内容"},
                {"汽油弹", "WEAPON", "BLOCK", "危险物品违法内容"},
                {"诈骗", "FRAUD", "REVIEW", "欺诈风险内容"},
                {"杀猪盘", "FRAUD", "BLOCK", "诈骗违法内容"},
                {"免费提现", "FRAUD", "REVIEW", "欺诈诱导内容"},
                {"刷单", "FRAUD", "REVIEW", "黑产引流内容"},
                {"套现", "FRAUD", "REVIEW", "金融违规内容"},
                {"代办证件", "FRAUD", "BLOCK", "证件违法内容"},
                {"代开发票", "FRAUD", "BLOCK", "发票违法内容"},
                {"站外交易", "FRAUD", "REVIEW", "绕开平台交易"},
                {"加微信", "CONTACT", "REVIEW", "站外引流联系方式"},
                {"加QQ", "CONTACT", "REVIEW", "站外引流联系方式"},
                {"私下转账", "CONTACT", "REVIEW", "绕开平台交易"},
                {"低价私聊", "CONTACT", "REVIEW", "站外交易引导"},
                {"政治谣言", "POLITICAL", "BLOCK", "政治谣言风险"},
                {"煽动", "POLITICAL", "BLOCK", "煽动性违法内容"},
                {"邪教", "ILLEGAL", "BLOCK", "非法组织相关内容"},
                {"传销", "ILLEGAL", "BLOCK", "非法经营相关内容"},
                {"非法集资", "ILLEGAL", "BLOCK", "非法金融活动"},
                {"人身攻击", "ABUSE", "REVIEW", "辱骂攻击内容"},
                {"辱骂", "ABUSE", "REVIEW", "不友善互动内容"},
                {"地域黑", "ABUSE", "REVIEW", "歧视攻击内容"},
                {"威胁他人", "VIOLENCE", "BLOCK", "暴力威胁内容"},
                {"暴力威胁", "VIOLENCE", "BLOCK", "暴力威胁内容"},
                {"打砸", "VIOLENCE", "BLOCK", "暴力违法内容"},
                {"虚假低价团", "TRAVEL_RISK", "REVIEW", "旅游业务风险内容"},
                {"强制购物", "TRAVEL_RISK", "REVIEW", "旅游业务风险内容"},
                {"甩团", "TRAVEL_RISK", "REVIEW", "旅游安全风险内容"},
                {"黑车", "TRAVEL_RISK", "REVIEW", "旅游安全风险内容"},
                {"逃票攻略", "TRAVEL_RISK", "BLOCK", "违法违规旅游内容"},
                {"无证导游", "TRAVEL_RISK", "REVIEW", "旅游服务资质风险"},
                {"低价陷阱", "TRAVEL_RISK", "REVIEW", "旅游消费风险"},
                {"购物返点", "TRAVEL_RISK", "REVIEW", "旅游购物风险"}
        };

        for (Object[] row : rows) {
            jdbcTemplate.update("""
                    INSERT IGNORE INTO sensitive_word
                    (`word`, `category`, `level`, `match_type`, `replacement`, `remark`, `status`, `create_time`, `update_time`)
                    VALUES (?, ?, ?, 'CONTAINS', '***', ?, 1, NOW(), NOW())
                    """, row[0], row[1], row[2], row[3]);
        }
    }
}
