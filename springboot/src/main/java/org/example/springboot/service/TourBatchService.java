package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourBatchMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TourBatchService {

    @Resource
    private TourBatchMapper tourBatchMapper;

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
        tourBatchMapper.insert(tourBatch);
    }

    /**
     * 批量新增出发班期
     */
    @Transactional
    public void addBatch(List<TourBatch> tourBatches) {
        for (TourBatch batch : tourBatches) {
            normalizeAndValidateCapacity(batch);
            tourBatchMapper.insert(batch);
        }
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
    }

    /**
     * 删除出发班期
     */
    public void delete(Long id) {
        tourBatchMapper.deleteById(id);
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
}
