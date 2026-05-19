package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Tour;
import org.example.springboot.entity.TourCollection;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourCollectionMapper;
import org.example.springboot.mapper.TourMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TourCollectionService {
    @Resource
    private TourCollectionMapper tourCollectionMapper;

    @Resource
    private TourMapper tourMapper;

    public void addCollection(Long tourId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录后再收藏行程");
        }
        Tour tour = tourMapper.selectById(tourId);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }
        if (isCollected(tourId)) {
            throw new ServiceException("已收藏该行程");
        }

        TourCollection collection = new TourCollection();
        collection.setUserId(currentUser.getId());
        collection.setTourId(tourId);
        collection.setCreateTime(LocalDateTime.now());
        if (tourCollectionMapper.insert(collection) <= 0) {
            throw new ServiceException("收藏失败");
        }
    }

    public void cancelCollection(Long tourId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        LambdaQueryWrapper<TourCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourCollection::getUserId, currentUser.getId())
                .eq(TourCollection::getTourId, tourId);
        if (tourCollectionMapper.delete(wrapper) <= 0) {
            throw new ServiceException("取消收藏失败，可能未收藏该行程");
        }
    }

    public boolean isCollected(Long tourId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        LambdaQueryWrapper<TourCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourCollection::getUserId, currentUser.getId())
                .eq(TourCollection::getTourId, tourId);
        return tourCollectionMapper.selectCount(wrapper) > 0;
    }

    public Page<TourCollection> getUserCollections(Long userId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<TourCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourCollection::getUserId, userId)
                .orderByDesc(TourCollection::getCreateTime);
        Page<TourCollection> page = tourCollectionMapper.selectPage(new Page<>(currentPage, size), wrapper);
        fillTourInfo(page.getRecords());
        return page;
    }

    public List<Long> getUserCollectionIds(Long userId) {
        LambdaQueryWrapper<TourCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourCollection::getUserId, userId)
                .select(TourCollection::getTourId);
        return tourCollectionMapper.selectList(wrapper).stream()
                .map(TourCollection::getTourId)
                .toList();
    }

    private void fillTourInfo(List<TourCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return;
        }
        List<Long> tourIds = collections.stream()
                .map(TourCollection::getTourId)
                .distinct()
                .collect(Collectors.toList());
        if (tourIds.isEmpty()) {
            return;
        }
        Map<Long, Tour> tourMap = tourMapper.selectBatchIds(tourIds).stream()
                .collect(Collectors.toMap(Tour::getId, item -> item));
        collections.forEach(collection -> collection.setTourInfo(tourMap.get(collection.getTourId())));
    }
}
