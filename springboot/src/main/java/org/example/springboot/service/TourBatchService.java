package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourBatchMapper;
import org.example.springboot.mapper.TourMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TourBatchService {

    @Resource
    private TourBatchMapper tourBatchMapper;

    @Resource
    private TourMapper tourMapper;

    /**
     * 根据行程ID获取出发班期列表
     */
    public List<TourBatch> getByTourId(Long tourId) {
        LambdaQueryWrapper<TourBatch> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourBatch::getTourId, tourId);
        queryWrapper.orderByAsc(TourBatch::getDepartureDate);
        return tourBatchMapper.selectList(queryWrapper);
    }

    /**
     * 新增出发班期
     */
    public void add(TourBatch tourBatch) {
        normalizeAndValidateCapacity(tourBatch);
        if (restoreOrSkipExistingBatch(tourBatch)) {
            syncTourTravelDates(tourBatch.getTourId());
            return;
        }
        tourBatchMapper.insert(tourBatch);
        syncTourTravelDates(tourBatch.getTourId());
    }

    /**
     * 批量新增出发班期
     */
    @Transactional
    public void addBatch(List<TourBatch> tourBatches) {
        Long tourId = null;
        for (TourBatch batch : tourBatches) {
            normalizeAndValidateCapacity(batch);
            if (tourId == null) {
                tourId = batch.getTourId();
            }
            if (restoreOrSkipExistingBatch(batch)) {
                continue;
            }
            tourBatchMapper.insert(batch);
        }
        syncTourTravelDates(tourId);
    }

    /**
     * 更新出发班期
     */
    public void update(TourBatch tourBatch) {
        TourBatch exist = tourBatchMapper.selectById(tourBatch.getId());
        if (exist == null) {
            throw new ServiceException("出发班期不存在");
        }
        normalizeAndValidateCapacity(tourBatch);
        tourBatchMapper.updateById(tourBatch);
        syncTourTravelDates(tourBatch.getTourId() != null ? tourBatch.getTourId() : exist.getTourId());
    }

    /**
     * 删除出发班期
     */
    public void delete(Long id) {
        TourBatch existing = tourBatchMapper.selectById(id);
        tourBatchMapper.deleteById(id);
        if (existing != null) {
            syncTourTravelDates(existing.getTourId());
        }
    }

    private void normalizeAndValidateCapacity(TourBatch batch) {
        int occupied = batch.getOccupied() == null ? 0 : batch.getOccupied();
        int remaining = batch.getRemaining() == null ? 0 : batch.getRemaining();
        int maxCapacity = batch.getMaxCapacity() == null ? 0 : batch.getMaxCapacity();

        if (occupied < 0 || remaining < 0 || maxCapacity < 1) {
            throw new ServiceException("班期容量数据不合法");
        }
        if (remaining < occupied) {
            throw new ServiceException("余位不能小于已锁定名额");
        }
        if (remaining > maxCapacity) {
            throw new ServiceException("余位不能大于最大容量");
        }

        batch.setOccupied(occupied);
        batch.setRemaining(remaining);
        batch.setMaxCapacity(maxCapacity);
        if (batch.getStatus() == null || batch.getStatus().isBlank()) {
            batch.setStatus("可报名");
        }
    }

    private boolean restoreOrSkipExistingBatch(TourBatch batch) {
        if (batch == null || batch.getTourId() == null || batch.getDepartureDate() == null) {
            return false;
        }
        TourBatch existing = tourBatchMapper.selectOne(new LambdaQueryWrapper<TourBatch>()
                .eq(TourBatch::getTourId, batch.getTourId())
                .eq(TourBatch::getDepartureDate, batch.getDepartureDate())
                .last("LIMIT 1"));
        if (existing == null) {
            return false;
        }
        if ("已取消".equals(existing.getStatus())) {
            existing.setStatus(batch.getStatus());
            existing.setRemaining(batch.getRemaining());
            existing.setMaxCapacity(batch.getMaxCapacity());
            existing.setAdultDateExtraFee(batch.getAdultDateExtraFee());
            existing.setChildDateExtraFee(batch.getChildDateExtraFee());
            tourBatchMapper.updateById(existing);
        }
        return true;
    }

    private void syncTourTravelDates(Long tourId) {
        if (tourId == null) {
            return;
        }
        List<LocalDate> dates = tourBatchMapper.selectList(new LambdaQueryWrapper<TourBatch>()
                        .eq(TourBatch::getTourId, tourId)
                        .ge(TourBatch::getDepartureDate, LocalDate.now())
                        .ne(TourBatch::getStatus, "已取消")
                        .orderByAsc(TourBatch::getDepartureDate)
                        .orderByAsc(TourBatch::getId))
                .stream()
                .map(TourBatch::getDepartureDate)
                .filter(date -> date != null)
                .distinct()
                .collect(Collectors.toList());

        org.example.springboot.entity.Tour tour = new org.example.springboot.entity.Tour();
        tour.setId(tourId);
        if (dates.isEmpty()) {
            tour.setRecommendDate("");
            tour.setMoreDates("");
        } else {
            tour.setRecommendDate(dates.get(0).toString());
            tour.setMoreDates(dates.stream()
                    .skip(1)
                    .map(date -> String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth()))
                    .collect(Collectors.joining("、")));
        }
        tourMapper.updateById(tour);
    }
}
