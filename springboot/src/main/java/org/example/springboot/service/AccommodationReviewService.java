package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Accommodation;
import org.example.springboot.entity.AccommodationReview;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.AccommodationMapper;
import org.example.springboot.mapper.AccommodationReviewMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccommodationReviewService {
    
    @Resource
    private AccommodationReviewMapper reviewMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private AccommodationMapper accommodationMapper;
    @Resource
    private SensitiveWordService sensitiveWordService;
    
    /**
     * 分页查询住宿评价
     * @param accommodationId 住宿ID
     * @param currentPage 当前页码
     * @param size 每页记录数
     * @return 分页数据
     */
    public Page<AccommodationReview> getReviewsByPage(Integer accommodationId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<AccommodationReview> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (accommodationId != null) {
            queryWrapper.eq(AccommodationReview::getAccommodationId, accommodationId);
        }
        queryWrapper.eq(AccommodationReview::getReviewStatus, ContentReviewService.STATUS_APPROVED);
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(AccommodationReview::getCreateTime);
        
        Page<AccommodationReview> page = reviewMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 获取关联的用户信息
        List<Long> userIds = page.getRecords().stream()
                .map(AccommodationReview::getUserId)
                .collect(Collectors.toList());
        
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
            
            // 设置用户信息
            page.getRecords().forEach(review -> {
                User user = userMap.get(review.getUserId());
                if (user != null) {
                    review.setNickname(user.getNickname());
                    review.setAvatar(user.getAvatar());
                }
            });
        }
        
        return page;
    }

    public Page<AccommodationReview> getMyReviews(Long userId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<AccommodationReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AccommodationReview::getUserId, userId)
                .orderByDesc(AccommodationReview::getCreateTime);

        Page<AccommodationReview> page = reviewMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        fillAccommodationInfo(page.getRecords());
        return page;
    }
    
    /**
     * 添加住宿评价
     * @param review 评价信息
     * @return 是否成功
     */
    public boolean addReview(AccommodationReview review) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        review.setUserId((long) currentUser.getId().intValue());
        review.setContent(sensitiveWordService.filterContent(review.getContent(), "ACCOMMODATION_REVIEW",
                review.getAccommodationId() == null ? null : String.valueOf(review.getAccommodationId())));
        review.setReviewStatus(ContentReviewService.STATUS_PENDING);
        review.setCreateTime(LocalDateTime.now());
        
        boolean result = reviewMapper.insert(review) > 0;
        
        if (result) {
            // 通过敏感词过滤后进入人工审核，审核通过后再展示并计入评分
        }
        
        return result;
    }
    
    /**
     * 更新住宿的平均评分
     * @param accommodationId 住宿ID
     */
    private void updateAccommodationRating(Integer accommodationId) {
        LambdaQueryWrapper<AccommodationReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AccommodationReview::getAccommodationId, accommodationId);
        queryWrapper.eq(AccommodationReview::getReviewStatus, ContentReviewService.STATUS_APPROVED);
        
        // 查询该住宿的所有评价
        List<AccommodationReview> reviews = reviewMapper.selectList(queryWrapper);
        
        if (!reviews.isEmpty()) {
            // 计算平均评分
            BigDecimal totalRating = BigDecimal.ZERO;
            for (AccommodationReview review : reviews) {
                totalRating = totalRating.add(review.getRating());
            }
            
            BigDecimal averageRating = totalRating.divide(new BigDecimal(reviews.size()), 1, RoundingMode.HALF_UP);
            
            // 更新住宿的评分
            org.example.springboot.entity.Accommodation accommodation = accommodationMapper.selectById(accommodationId);
            if (accommodation != null) {
                accommodation.setStarLevel(averageRating);
                accommodationMapper.updateById(accommodation);
            }
        }
    }

    private void fillAccommodationInfo(List<AccommodationReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }
        List<Long> accommodationIds = reviews.stream()
                .map(AccommodationReview::getAccommodationId)
                .filter(id -> id != null)
                .map(Integer::longValue)
                .distinct()
                .collect(Collectors.toList());
        if (accommodationIds.isEmpty()) {
            return;
        }

        List<Accommodation> accommodations = accommodationMapper.selectBatchIds(accommodationIds);
        Map<Long, Accommodation> accommodationMap = accommodations.stream()
                .collect(Collectors.toMap(Accommodation::getId, item -> item));

        reviews.forEach(review -> {
            Accommodation accommodation = review.getAccommodationId() == null
                    ? null
                    : accommodationMap.get(review.getAccommodationId().longValue());
            if (accommodation != null) {
                review.setAccommodationName(accommodation.getName());
            }
        });
    }
    
    /**
     * 删除评价
     * @param id 评价ID
     * @return 是否成功
     */
    public boolean deleteReview(Integer id) {
        AccommodationReview review = reviewMapper.selectById(id);
        if (review == null) {
            return false;
        }
        
        // 检查权限（只有管理员或评价的发布者可以删除）
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || 
            (!currentUser.getRoleCode().equals("ADMIN") && !currentUser.getId().equals(review.getUserId().longValue()))) {
            return false;
        }
        
        boolean result = reviewMapper.deleteById(id) > 0;
        
        if (result) {
            // 更新住宿的平均评分
            updateAccommodationRating(review.getAccommodationId());
        }
        
        return result;
    }
} 
