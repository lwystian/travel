package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TourPackage;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourPackageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TourPackageService {

    @Resource
    private TourPackageMapper tourPackageMapper;

    /**
     * 根据行程ID获取套餐列表
     */
    public List<TourPackage> getByTourId(Long tourId) {
        LambdaQueryWrapper<TourPackage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourPackage::getTourId, tourId);
        queryWrapper.orderByAsc(TourPackage::getSortOrder);
        return tourPackageMapper.selectList(queryWrapper);
    }

    /**
     * 新增套餐
     */
    public void add(TourPackage tourPackage) {
        if (tourPackage.getStatus() == null) {
            tourPackage.setStatus(1);
        }
        tourPackageMapper.insert(tourPackage);
    }

    /**
     * 更新套餐
     */
    public void update(TourPackage tourPackage) {
        TourPackage exist = tourPackageMapper.selectById(tourPackage.getId());
        if (exist == null) {
            throw new ServiceException("套餐不存在");
        }
        tourPackageMapper.updateById(tourPackage);
    }

    /**
     * 删除套餐
     */
    public void delete(Long id) {
        tourPackageMapper.deleteById(id);
    }
}
