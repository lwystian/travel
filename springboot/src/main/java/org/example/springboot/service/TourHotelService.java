package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Accommodation;
import org.example.springboot.entity.TourHotel;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AccommodationMapper;
import org.example.springboot.mapper.TourHotelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TourHotelService {

    @Resource
    private TourHotelMapper tourHotelMapper;

    @Resource
    private AccommodationMapper accommodationMapper;

    /**
     * 获取行程关联的酒店列表
     */
    public List<TourHotel> getTourHotels(Long tourId) {
        LambdaQueryWrapper<TourHotel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourHotel::getTourId, tourId);
        queryWrapper.eq(TourHotel::getEnabled, 1);
        queryWrapper.orderByAsc(TourHotel::getCreateTime);
        return tourHotelMapper.selectList(queryWrapper);
    }

    /**
     * 获取行程关联的酒店列表（分页，包含已禁用的）
     */
    public Page<TourHotel> getTourHotelsByPage(Long tourId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<TourHotel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourHotel::getTourId, tourId);
        queryWrapper.orderByDesc(TourHotel::getCreateTime);
        return tourHotelMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }

    /**
     * 获取酒店详情
     */
    public TourHotel getTourHotelById(Long id) {
        TourHotel tourHotel = tourHotelMapper.selectById(id);
        if (tourHotel == null) {
            throw new ServiceException("酒店关联不存在");
        }
        return tourHotel;
    }

    /**
     * 添加行程酒店关联
     */
    @Transactional
    public TourHotel addTourHotel(TourHotel tourHotel) {
        if (tourHotel.getTourId() == null) {
            throw new ServiceException("行程ID不能为空");
        }
        if (tourHotel.getName() == null || tourHotel.getName().isEmpty()) {
            throw new ServiceException("酒店名称不能为空");
        }
        // 如果提供了住宿ID，填充住宿信息
        if (tourHotel.getAccommodationId() != null) {
            Accommodation accommodation = accommodationMapper.selectById(tourHotel.getAccommodationId());
            if (accommodation != null) {
                if (tourHotel.getName() == null || tourHotel.getName().isEmpty()) {
                    tourHotel.setName(accommodation.getName());
                }
                if (tourHotel.getType() == null || tourHotel.getType().isEmpty()) {
                    tourHotel.setType(accommodation.getType());
                }
                if (tourHotel.getImageUrl() == null || tourHotel.getImageUrl().isEmpty()) {
                    tourHotel.setImageUrl(accommodation.getImageUrl());
                }
                if (tourHotel.getStarLevel() == null) {
                    tourHotel.setStarLevel(accommodation.getStarLevel());
                }
                // 如果没有设置价格，从住宿价格区间提取
                if (tourHotel.getPricePerNight() == null || tourHotel.getPricePerNight().compareTo(BigDecimal.ZERO) == 0) {
                    String priceRange = accommodation.getPriceRange();
                    if (priceRange != null && !priceRange.isEmpty()) {
                        try {
                            String price = priceRange.replaceAll("[^0-9]", "");
                            if (!price.isEmpty()) {
                                tourHotel.setPricePerNight(new BigDecimal(price));
                            }
                        } catch (Exception e) {
                            // 解析价格失败，使用默认值
                        }
                    }
                }
            }
        }
        // 设置默认值
        if (tourHotel.getEnabled() == null) {
            tourHotel.setEnabled(1);
        }
        if (tourHotel.getDays() == null) {
            tourHotel.setDays(1);
        }
        if (tourHotel.getPricePerNight() == null) {
            tourHotel.setPricePerNight(BigDecimal.ZERO);
        }
        tourHotel.setCreateTime(LocalDateTime.now());
        tourHotel.setUpdateTime(LocalDateTime.now());
        tourHotelMapper.insert(tourHotel);
        // MyBatis-Plus 会自动将生成的ID设置到tourHotel对象中
        return tourHotel;
    }

    /**
     * 更新行程酒店关联
     */
    @Transactional
    public TourHotel updateTourHotel(TourHotel tourHotel) {
        TourHotel existTourHotel = tourHotelMapper.selectById(tourHotel.getId());
        if (existTourHotel == null) {
            throw new ServiceException("酒店关联不存在");
        }
        tourHotel.setUpdateTime(LocalDateTime.now());
        // 保留创建时间
        tourHotel.setCreateTime(existTourHotel.getCreateTime());
        tourHotelMapper.updateById(tourHotel);
        return tourHotelMapper.selectById(tourHotel.getId());
    }

    /**
     * 删除行程酒店关联
     */
    @Transactional
    public boolean deleteTourHotel(Long id) {
        return tourHotelMapper.deleteById(id) > 0;
    }

    /**
     * 获取行程的可选酒店列表（用于前端展示）
     */
    public List<TourHotel> getAvailableHotelsForTour(Long tourId) {
        // 返回启用的酒店列表
        return getTourHotels(tourId);
    }
}
