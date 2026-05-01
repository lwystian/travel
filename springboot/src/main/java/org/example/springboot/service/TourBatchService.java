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
        tourBatchMapper.insert(tourBatch);
    }

    /**
     * 批量新增出发班期
     */
    @Transactional
    public void addBatch(List<TourBatch> tourBatches) {
        for (TourBatch batch : tourBatches) {
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
        tourBatchMapper.updateById(tourBatch);
    }

    /**
     * 删除出发班期
     */
    public void delete(Long id) {
        tourBatchMapper.deleteById(id);
    }
}
