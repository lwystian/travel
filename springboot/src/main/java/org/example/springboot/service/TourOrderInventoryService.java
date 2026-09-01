package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.mapper.TourBatchMapper;
import org.springframework.stereotype.Service;

@Service
public class TourOrderInventoryService {
    @Resource
    private TourBatchMapper tourBatchMapper;

    public TourBatch findBatch(TourOrder order) {
        if (order == null) return null;
        if (order.getBatchId() != null) {
            TourBatch batch = tourBatchMapper.selectById(order.getBatchId());
            if (batch != null) return batch;
        }
        return tourBatchMapper.selectOne(new LambdaQueryWrapper<TourBatch>()
                .eq(TourBatch::getTourId, order.getTourId())
                .eq(TourBatch::getDepartureDate, order.getDepartureDate()));
    }

    public boolean confirm(TourOrder order) {
        TourBatch batch = findBatch(order);
        int people = people(order);
        return batch != null && people > 0 && tourBatchMapper.confirmOccupancy(batch.getId(), people) > 0;
    }

    public Long release(TourOrder order) {
        TourBatch batch = findBatch(order);
        if (batch == null) return null;
        tourBatchMapper.releaseOccupancy(batch.getId(), people(order));
        return batch.getId();
    }

    public Long refund(TourOrder order) {
        TourBatch batch = findBatch(order);
        if (batch == null) return null;
        int maxCapacity = batch.getMaxCapacity() == null ? 999 : batch.getMaxCapacity();
        tourBatchMapper.returnRemaining(batch.getId(), people(order), maxCapacity);
        return batch.getId();
    }

    private int people(TourOrder order) {
        return safe(order.getAdultCount()) + safe(order.getChildCount());
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
