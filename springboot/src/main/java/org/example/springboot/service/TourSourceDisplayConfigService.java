package org.example.springboot.service;

import jakarta.annotation.Resource;
import org.example.springboot.exception.ServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;

@Service
public class TourSourceDisplayConfigService {
    @Resource
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> apply(
            String sourceType,
            List<Map<String, Object>> source,
            boolean includeHidden) {
        Map<String, DisplayConfig> configs = loadConfigs(sourceType);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> sourceItem : source) {
            Map<String, Object> item = new LinkedHashMap<>(sourceItem);
            String sourceTourId = text(item.get("sourceId"));
            int sourceSortOrder = integer(item.get("sortOrder"));
            DisplayConfig config = configs.get(sourceTourId);
            boolean visible = config == null || config.visible();
            int sortOrder = config == null ? sourceSortOrder : config.sortOrder();

            item.put("sourceStatus", integer(item.get("status")));
            item.put("sourceSortOrder", sourceSortOrder);
            item.put("websiteVisible", visible);
            item.put("sortOrder", sortOrder);
            item.put("status", visible ? 1 : 0);
            if (includeHidden || visible) {
                result.add(item);
            }
        }
        result.sort(defaultComparator());
        return result;
    }

    public boolean isVisible(String sourceType, String sourceTourId) {
        DisplayConfig config = loadConfigs(sourceType).get(sourceTourId);
        return config == null || config.visible();
    }

    public void save(String sourceType, String sourceTourId, boolean visible, int sortOrder) {
        String normalizedType = text(sourceType);
        String normalizedTourId = text(sourceTourId);
        if (normalizedType.isBlank() || normalizedType.length() > 20) {
            throw new ServiceException("商品来源无效");
        }
        if (normalizedTourId.isBlank() || normalizedTourId.length() > 255) {
            throw new ServiceException("小程序商品编号无效");
        }
        jdbcTemplate.update("""
                INSERT INTO `tour_source_display_config`
                    (`source_type`, `source_tour_id`, `visible`, `sort_order`)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    `visible` = VALUES(`visible`),
                    `sort_order` = VALUES(`sort_order`),
                    `update_time` = CURRENT_TIMESTAMP
                """, normalizedType, normalizedTourId, visible ? 1 : 0, sortOrder);
    }

    @Transactional
    public void reorder(String sourceType, List<String> sourceTourIds, int startOrder) {
        String normalizedType = text(sourceType);
        if (normalizedType.isBlank() || normalizedType.length() > 20) {
            throw new ServiceException("商品来源无效");
        }
        if (sourceTourIds == null || sourceTourIds.isEmpty() || sourceTourIds.size() > 500) {
            throw new ServiceException("拖拽排序商品数量无效");
        }
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>();
        for (String sourceTourId : sourceTourIds) {
            String normalizedId = text(sourceTourId);
            if (normalizedId.isBlank() || normalizedId.length() > 255 || !normalizedIds.add(normalizedId)) {
                throw new ServiceException("拖拽排序商品编号无效");
            }
        }
        int sortOrder = startOrder;
        for (String sourceTourId : normalizedIds) {
            jdbcTemplate.update("""
                    INSERT INTO `tour_source_display_config`
                        (`source_type`, `source_tour_id`, `visible`, `sort_order`)
                    VALUES (?, ?, 1, ?)
                    ON DUPLICATE KEY UPDATE
                        `sort_order` = VALUES(`sort_order`),
                        `update_time` = CURRENT_TIMESTAMP
                    """, normalizedType, sourceTourId, sortOrder++);
        }
    }

    public Comparator<Map<String, Object>> defaultComparator() {
        return Comparator
                .comparingInt((Map<String, Object> item) -> integer(item.get("sortOrder")))
                .thenComparingInt(item -> integer(item.get("sourceSortOrder")))
                .thenComparing(item -> text(item.get("sourceId")));
    }

    private Map<String, DisplayConfig> loadConfigs(String sourceType) {
        Map<String, DisplayConfig> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT `source_tour_id`, `visible`, `sort_order`
                FROM `tour_source_display_config`
                WHERE `source_type` = ?
                """, sourceType);
        for (Map<String, Object> row : rows) {
            result.put(text(row.get("source_tour_id")), new DisplayConfig(
                    integer(row.get("visible")) == 1,
                    integer(row.get("sort_order"))
            ));
        }
        return result;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int integer(Object value) {
        if (value instanceof Boolean bool) return bool ? 1 : 0;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private record DisplayConfig(boolean visible, int sortOrder) {
    }
}
