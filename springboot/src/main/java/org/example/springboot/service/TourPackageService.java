package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.TourPackage;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourPackageMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        normalizeAndValidate(tourPackage);
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
        normalizeAndValidate(tourPackage);
        tourPackageMapper.updateById(tourPackage);
    }

    /**
     * 删除套餐
     */
    public void delete(Long id) {
        tourPackageMapper.deleteById(id);
    }

    private void normalizeAndValidate(TourPackage tourPackage) {
        if (tourPackage == null) {
            throw new ServiceException("套餐不能为空");
        }
        if (tourPackage.getTourId() == null) {
            throw new ServiceException("行程ID不能为空");
        }
        if (StringUtils.isBlank(tourPackage.getName())) {
            throw new ServiceException("套餐名称不能为空");
        }
        tourPackage.setName(tourPackage.getName().trim());
        tourPackage.setAdultPrice(nonNegativeAmount(tourPackage.getAdultPrice(), "成人售价不能小于0"));
        tourPackage.setChildPrice(nonNegativeAmount(tourPackage.getChildPrice(), "儿童售价不能小于0"));
        validateOriginalPrice(tourPackage.getOriginalAdultPrice(), tourPackage.getAdultPrice(), "成人划线价必须高于成人售价");
        validateOriginalPrice(tourPackage.getOriginalChildPrice(), tourPackage.getChildPrice(), "儿童划线价必须高于儿童售价");
        if (tourPackage.getSortOrder() == null) {
            tourPackage.setSortOrder(0);
        }
    }

    private BigDecimal nonNegativeAmount(BigDecimal value, String message) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(message);
        }
        return amount;
    }

    private void validateOriginalPrice(BigDecimal originalPrice, BigDecimal salePrice, String message) {
        if (originalPrice == null) {
            return;
        }
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0 || originalPrice.compareTo(salePrice) <= 0) {
            throw new ServiceException(message);
        }
    }
}
