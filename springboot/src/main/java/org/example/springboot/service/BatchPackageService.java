package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.BatchPackage;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.BatchPackageMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BatchPackageService {

    @Resource
    private BatchPackageMapper batchPackageMapper;

    /**
     * 根据行程ID获取批次套餐列表
     */
    public List<BatchPackage> getByTourId(Long tourId) {
        LambdaQueryWrapper<BatchPackage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BatchPackage::getTourId, tourId);
        queryWrapper.orderByAsc(BatchPackage::getSortOrder);
        return batchPackageMapper.selectList(queryWrapper);
    }

    /**
     * 新增批次套餐
     */
    public void add(BatchPackage batchPackage) {
        normalizeAndValidate(batchPackage);
        if (batchPackage.getStatus() == null) {
            batchPackage.setStatus(1);
        }
        batchPackageMapper.insert(batchPackage);
    }

    /**
     * 更新批次套餐
     */
    public void update(BatchPackage batchPackage) {
        BatchPackage exist = batchPackageMapper.selectById(batchPackage.getId());
        if (exist == null) {
            throw new ServiceException("批次套餐不存在");
        }
        normalizeAndValidate(batchPackage);
        batchPackageMapper.updateById(batchPackage);
    }

    /**
     * 删除批次套餐
     */
    public void delete(Long id) {
        batchPackageMapper.deleteById(id);
    }

    private void normalizeAndValidate(BatchPackage batchPackage) {
        if (batchPackage == null) {
            throw new ServiceException("附加费用不能为空");
        }
        if (batchPackage.getTourId() == null) {
            throw new ServiceException("行程ID不能为空");
        }
        if (StringUtils.isBlank(batchPackage.getName())) {
            throw new ServiceException("附加费用名称不能为空");
        }
        batchPackage.setName(batchPackage.getName().trim());
        if (batchPackage.getExtraFeePerPerson() == null || batchPackage.getExtraFeePerPerson().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("附加费用售价必须大于0");
        }
        if (batchPackage.getSortOrder() == null) {
            batchPackage.setSortOrder(0);
        }
    }
}
