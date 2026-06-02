package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TourSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TourSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        addColumn("detail_content", "ALTER TABLE `tour` ADD COLUMN `detail_content` LONGTEXT DEFAULT NULL COMMENT '行程详细' AFTER `notice`");
        normalizeTourCodes();
        initTourCodeSequenceTable();
        seedTourCodeSequences();
        addUniqueIndex("uk_tour_code", "ALTER TABLE `tour` ADD UNIQUE INDEX `uk_tour_code` (`code`)");
    }

    private void addColumn(@NonNull String columnName, @NonNull String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'tour'
                      AND COLUMN_NAME = ?
                    """, Integer.class, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(Objects.requireNonNull(sql, "schema sql must not be null"));
            }
        } catch (Exception e) {
            LOGGER.warn("Initialize tour column failed: {}", columnName, e);
        }
    }

    private void normalizeTourCodes() {
        try {
            List<Map<String, Object>> tours = jdbcTemplate.queryForList("""
                    SELECT id, code, tour_type
                    FROM `tour`
                    ORDER BY id ASC
                    """);
            java.util.Set<String> usedCodes = new java.util.HashSet<>();
            java.util.Map<String, Integer> maxSeqByPrefix = new java.util.HashMap<>();
            java.util.List<Map<String, Object>> needsCode = new java.util.ArrayList<>();

            for (Map<String, Object> tour : tours) {
                String code = stringValue(tour.get("code"));
                if (!hasText(code) || usedCodes.contains(code)) {
                    needsCode.add(tour);
                    continue;
                }
                usedCodes.add(code);
                int dashIndex = code.indexOf('-');
                if (dashIndex > 0 && dashIndex < code.length() - 1) {
                    try {
                        String prefix = code.substring(0, dashIndex);
                        int seq = Integer.parseInt(code.substring(dashIndex + 1));
                        maxSeqByPrefix.merge(prefix, seq, this::maxSequence);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            for (Map<String, Object> tour : needsCode) {
                String prefix = resolvePrefix(stringValue(tour.get("tour_type")));
                int nextSeq = maxSeqByPrefix.getOrDefault(prefix, 0) + 1;
                String newCode = String.format("%s-%04d", prefix, nextSeq);
                while (usedCodes.contains(newCode)) {
                    nextSeq++;
                    newCode = String.format("%s-%04d", prefix, nextSeq);
                }
                maxSeqByPrefix.put(prefix, nextSeq);
                usedCodes.add(newCode);
                jdbcTemplate.update("UPDATE `tour` SET `code` = ? WHERE `id` = ?", newCode, tour.get("id"));
            }
        } catch (Exception e) {
            LOGGER.warn("Normalize tour codes failed", e);
        }
    }

    private void addUniqueIndex(@NonNull String indexName, @NonNull String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'tour'
                      AND INDEX_NAME = ?
                    """, Integer.class, indexName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(Objects.requireNonNull(sql, "schema sql must not be null"));
            }
        } catch (Exception e) {
            LOGGER.warn("Initialize tour index failed: {}", indexName, e);
        }
    }

    private void initTourCodeSequenceTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `tour_code_sequence` (
                      `prefix` VARCHAR(16) NOT NULL COMMENT '行程编号前缀',
                      `current_seq` INT NOT NULL DEFAULT 0 COMMENT '当前流水号',
                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                      `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (`prefix`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程业务编号流水表'
                    """);
        } catch (Exception e) {
            LOGGER.warn("Initialize tour code sequence table failed", e);
        }
    }

    private void seedTourCodeSequences() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT code
                    FROM `tour`
                    WHERE code IS NOT NULL AND code <> ''
                    """);
            java.util.Map<String, Integer> maxSeqByPrefix = new java.util.HashMap<>();
            for (Map<String, Object> row : rows) {
                String code = stringValue(row.get("code"));
                int dashIndex = code.indexOf('-');
                if (dashIndex <= 0 || dashIndex >= code.length() - 1) {
                    continue;
                }
                try {
                    String prefix = code.substring(0, dashIndex);
                    int seq = Integer.parseInt(code.substring(dashIndex + 1));
                    maxSeqByPrefix.merge(prefix, seq, this::maxSequence);
                } catch (NumberFormatException ignored) {
                }
            }
            for (Map.Entry<String, Integer> entry : maxSeqByPrefix.entrySet()) {
                jdbcTemplate.update("""
                        INSERT INTO `tour_code_sequence` (`prefix`, `current_seq`)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE `current_seq` = GREATEST(`current_seq`, VALUES(`current_seq`))
                        """, entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            LOGGER.warn("Seed tour code sequences failed", e);
        }
    }

    private String resolvePrefix(String tourType) {
        if (!hasText(tourType)) {
            return "TR";
        }
        return switch (tourType.trim().toLowerCase()) {
            case "around", "周边", "周边游" -> "ZW";
            case "long", "长线", "长线游" -> "CX";
            case "team", "跟团", "跟团游" -> "GT";
            case "free", "自由行" -> "ZY";
            case "private", "私家团" -> "SJ";
            case "custom", "定制游" -> "DZ";
            case "local", "当地参团", "当地游" -> "DD";
            case "selfdrive", "自驾", "自驾游" -> "ZJ";
            case "parent_child", "亲子", "亲子游" -> "QZ";
            case "study", "研学", "研学游" -> "YX";
            case "photography", "摄影", "摄影游" -> "SY";
            case "outdoor", "徒步", "户外徒步" -> "HW";
            case "cruise", "邮轮", "邮轮出行" -> "YL";
            case "wellness", "康养", "康养度假" -> "KY";
            case "other", "其它", "其他" -> "QT";
            default -> "TR";
        };
    }

    private Integer maxSequence(Integer left, Integer right) {
        return Math.max(left == null ? 0 : left, right == null ? 0 : right);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
