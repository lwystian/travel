package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourBatchMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MiniappInventoryService {
    private static final int UNLIMITED_STOCK = 1_000_000;

    @Resource
    private TourBatchMapper tourBatchMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    public void applyLocalAllocations(Map<String, Object> detail) {
        String sourceTourId = text(detail.get("sourceId"));
        if (sourceTourId.isBlank()) return;

        Map<String, Allocation> allocations = loadAllocations(sourceTourId);
        for (Map<String, Object> schedule : mapList(detail.get("batchDates"))) {
            String sourceScheduleId = text(schedule.get("sourceId"));
            if (sourceScheduleId.isBlank()) continue;
            int remoteAvailable = integer(schedule.get("remoteAvailableStock"));
            boolean unlimited = bool(schedule.get("unlimitedStock"));
            Allocation allocation = allocations.getOrDefault(sourceScheduleId, Allocation.EMPTY);
            int remaining = unlimited ? UNLIMITED_STOCK : Math.max(remoteAvailable - allocation.committed(), 0);
            int occupied = allocation.pending();
            int effectiveAvailable = unlimited ? UNLIMITED_STOCK : Math.max(remaining - occupied, 0);
            schedule.put("remaining", remaining);
            schedule.put("occupied", occupied);
            schedule.put("availableStock", effectiveAvailable);
            if (!unlimited && effectiveAvailable <= 0 && "可报名".equals(text(schedule.get("status")))) {
                schedule.put("status", "已满员");
            }
        }
    }

    public TourBatch syncBatch(String sourceTourId, Map<String, Object> schedule) {
        String sourceScheduleId = text(schedule.get("sourceId"));
        if (sourceTourId == null || sourceTourId.isBlank() || sourceScheduleId.isBlank()) {
            throw new ServiceException("小程序商品或班期编号缺失，请刷新后重试");
        }
        LocalDate departureDate;
        try {
            departureDate = LocalDate.parse(text(schedule.get("date")));
        } catch (Exception ex) {
            throw new ServiceException("小程序班期日期无效，请联系管理员");
        }

        boolean unlimited = bool(schedule.get("unlimitedStock"));
        int remoteAvailable = unlimited
                ? UNLIMITED_STOCK
                : Math.max(integer(schedule.get("remoteAvailableStock")), 0);
        int committed = loadCommitted(sourceTourId, sourceScheduleId);
        TourBatch batch = findBatch(sourceTourId, sourceScheduleId);
        if (batch == null) {
            batch = new TourBatch();
            batch.setTourId(stableNegativeId("schedule", sourceTourId, sourceScheduleId));
            batch.setSourceType(MiniappTourAdapterService.SOURCE_TYPE);
            batch.setSourceTourId(sourceTourId);
            batch.setSourceScheduleId(sourceScheduleId);
            batch.setDepartureDate(departureDate);
            batch.setAdultDateExtraFee(java.math.BigDecimal.ZERO);
            batch.setChildDateExtraFee(java.math.BigDecimal.ZERO);
            batch.setOccupied(0);
            batch.setPackageIds(null);
            batch.setAddonIds(null);
            batch.setCreateTime(LocalDateTime.now());
            try {
                applyStock(batch, schedule, remoteAvailable, committed, 0);
                tourBatchMapper.insert(batch);
                return batch;
            } catch (DuplicateKeyException ignored) {
                batch = findBatch(sourceTourId, sourceScheduleId);
            }
        }
        if (batch == null) {
            throw new ServiceException("同步小程序库存失败，请稍后重试");
        }

        int occupied = batch.getOccupied() == null ? 0 : batch.getOccupied();
        int remaining = unlimited ? UNLIMITED_STOCK : Math.max(remoteAvailable - committed, 0);
        int maxCapacity = unlimited ? UNLIMITED_STOCK : Math.max(remoteAvailable, remaining);
        String status = resolveStatus(schedule, unlimited || remaining - occupied > 0);
        jdbcTemplate.update("""
                UPDATE tour_batch
                SET departure_date = ?, status = ?, remaining = ?, max_capacity = ?,
                    source_available_stock = ?, source_unlimited_stock = ?,
                    source_stock_updated_at = NOW(), update_time = NOW()
                WHERE id = ?
                """, departureDate, status, Math.max(remaining, occupied), maxCapacity,
                remoteAvailable, unlimited, batch.getId());
        return tourBatchMapper.selectById(batch.getId());
    }

    public long stableNegativeId(String entityType, String sourceTourId, String sourceId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((entityType + "\n" + sourceTourId + "\n" + sourceId)
                    .getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(bytes).getLong() & Long.MAX_VALUE;
            return -Math.max(value, 1L);
        } catch (Exception ex) {
            throw new IllegalStateException("Generate external product identifier failed", ex);
        }
    }

    private void applyStock(TourBatch batch, Map<String, Object> schedule,
                            int remoteAvailable, int committed, int occupied) {
        boolean unlimited = bool(schedule.get("unlimitedStock"));
        int remaining = unlimited ? UNLIMITED_STOCK : Math.max(remoteAvailable - committed, 0);
        batch.setStatus(resolveStatus(schedule, unlimited || remaining - occupied > 0));
        batch.setRemaining(Math.max(remaining, occupied));
        batch.setMaxCapacity(unlimited ? UNLIMITED_STOCK : Math.max(remoteAvailable, remaining));
        batch.setSourceAvailableStock(remoteAvailable);
        batch.setSourceUnlimitedStock(unlimited);
        batch.setSourceStockUpdatedAt(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
    }

    private String resolveStatus(Map<String, Object> schedule, boolean hasStock) {
        String status = text(schedule.get("status"));
        if (!"可报名".equals(status)) return status.isBlank() ? "已结束" : status;
        return hasStock ? "可报名" : "已满员";
    }

    private TourBatch findBatch(String sourceTourId, String sourceScheduleId) {
        return tourBatchMapper.selectOne(new LambdaQueryWrapper<TourBatch>()
                .eq(TourBatch::getSourceType, MiniappTourAdapterService.SOURCE_TYPE)
                .eq(TourBatch::getSourceTourId, sourceTourId)
                .eq(TourBatch::getSourceScheduleId, sourceScheduleId));
    }

    private Map<String, Allocation> loadAllocations(String sourceTourId) {
        Map<String, Allocation> result = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT source_schedule_id,
                       COALESCE(SUM(CASE WHEN status IN (1, 4) THEN adult_count + COALESCE(child_count, 0) ELSE 0 END), 0) committed,
                       COALESCE(SUM(CASE WHEN status = 0 THEN adult_count + COALESCE(child_count, 0) ELSE 0 END), 0) pending
                FROM tour_order
                WHERE source_type = ? AND source_tour_id = ? AND source_schedule_id IS NOT NULL
                GROUP BY source_schedule_id
                """, MiniappTourAdapterService.SOURCE_TYPE, sourceTourId);
        for (Map<String, Object> row : rows) {
            result.put(text(row.get("source_schedule_id")),
                    new Allocation(integer(row.get("committed")), integer(row.get("pending"))));
        }
        return result;
    }

    private int loadCommitted(String sourceTourId, String sourceScheduleId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(adult_count + COALESCE(child_count, 0)), 0)
                FROM tour_order
                WHERE source_type = ? AND source_tour_id = ? AND source_schedule_id = ? AND status IN (1, 4)
                """, Integer.class, MiniappTourAdapterService.SOURCE_TYPE, sourceTourId, sourceScheduleId);
        return count == null ? 0 : count;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(text(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean bool) return bool;
        return "1".equals(text(value)) || "true".equalsIgnoreCase(text(value));
    }

    private record Allocation(int committed, int pending) {
        private static final Allocation EMPTY = new Allocation(0, 0);
    }
}
