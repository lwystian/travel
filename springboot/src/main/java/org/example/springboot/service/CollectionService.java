package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Collection;
import org.example.springboot.entity.ScenicCollection;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.Tour;
import org.example.springboot.entity.TourCollection;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CollectionMapper;
import org.example.springboot.mapper.ScenicCollectionMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.TourCollectionMapper;
import org.example.springboot.mapper.TourMapper;
import org.example.springboot.mapper.TravelGuideMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CollectionService {
    @Resource
    private CollectionMapper collectionMapper;
    
    @Resource
    private TravelGuideMapper travelGuideMapper;
    
    @Resource
    private UserMapper userMapper;
    @Resource
    private ScenicCollectionMapper scenicCollectionMapper;
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    @Resource
    private TourCollectionMapper tourCollectionMapper;
    @Resource
    private TourMapper tourMapper;
    
    /**
     * 添加收藏
     */
    public void addCollection(Collection collection) {
        // 验证用户是否存在
        if (userMapper.selectById(collection.getUserId()) == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 验证攻略是否存在
        if (travelGuideMapper.selectById(collection.getGuideId()) == null) {
            throw new ServiceException("攻略不存在");
        }
        
        // 检查是否已收藏
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, collection.getUserId())
                   .eq(Collection::getGuideId, collection.getGuideId());
        if (collectionMapper.selectOne(queryWrapper) != null) {
            throw new ServiceException("已经收藏过该攻略");
        }
        
        // 添加收藏
        if (collectionMapper.insert(collection) <= 0) {
            throw new ServiceException("收藏失败");
        }
    }
    
    /**
     * 取消收藏
     */
    public void cancelCollection(Long userId, Long guideId) {
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, userId)
                   .eq(Collection::getGuideId, guideId);
        
        if (collectionMapper.delete(queryWrapper) <= 0) {
            throw new ServiceException("取消收藏失败");
        }
    }
    
    /**
     * 分页查询用户的收藏列表
     */
    public Page<Collection> getCollectionsByPage(Long userId, Integer currentPage, Integer size) {
        // 检查用户是否存在
        if (userMapper.selectById(userId) == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 查询用户收藏
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, userId)
                   .orderByDesc(Collection::getCreateTime);
        
        Page<Collection> page = collectionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充攻略信息
        fillGuideInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 检查用户是否已收藏某攻略
     */
    public boolean isCollected(Long userId, Long guideId) {
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collection::getUserId, userId)
                   .eq(Collection::getGuideId, guideId);
        return collectionMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 获取当前登录用户的收藏列表
     */
    public Page<Collection> getCurrentUserCollections(Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("未登录");
        }
        return getCollectionsByPage(currentUser.getId(), currentPage, size);
    }
    
    /**
     * 管理员查询所有收藏
     */
    public Page<Collection> getCollectionsByAdmin(String username, String guideTitle, Integer currentPage, Integer size) {
        // 首先获取符合条件的用户
        List<Long> userIds = new ArrayList<>();
        if (StringUtils.isNotBlank(username)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getUsername, username);
            List<User> users = userMapper.selectList(userQuery);
            if (!users.isEmpty()) {
                userIds = users.stream().map(User::getId).collect(Collectors.toList());
            } else {
                // 如果没找到用户，直接返回空结果
                return new Page<>(currentPage, size);
            }
        }
        
        // 查询符合条件的攻略
        List<Long> guideIds = new ArrayList<>();
        if (StringUtils.isNotBlank(guideTitle)) {
            LambdaQueryWrapper<TravelGuide> guideQuery = new LambdaQueryWrapper<>();
            guideQuery.like(TravelGuide::getTitle, guideTitle);
            List<TravelGuide> guides = travelGuideMapper.selectList(guideQuery);
            if (!guides.isEmpty()) {
                guideIds = guides.stream().map(TravelGuide::getId).collect(Collectors.toList());
            } else {
                // 如果没找到攻略，直接返回空结果
                return new Page<>(currentPage, size);
            }
        }
        
        // 构建查询条件
        LambdaQueryWrapper<Collection> queryWrapper = new LambdaQueryWrapper<>();
        if (!userIds.isEmpty()) {
            queryWrapper.in(Collection::getUserId, userIds);
        }
        if (!guideIds.isEmpty()) {
            queryWrapper.in(Collection::getGuideId, guideIds);
        }
        
        queryWrapper.orderByDesc(Collection::getCreateTime);
        Page<Collection> page = collectionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充攻略和用户信息
        fillGuideAndUserInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 管理员删除收藏
     */
    public void deleteCollection(Long id) {
        if (collectionMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除收藏失败");
        }
    }

    public Page<Map<String, Object>> getUnifiedCollectionsByAdmin(String type, String username, String keyword,
                                                                  Integer currentPage, Integer size) {
        List<Long> userIds = resolveUserIds(username);
        if (StringUtils.isNotBlank(username) && userIds.isEmpty()) {
            return emptyUnifiedPage(currentPage, size);
        }

        List<Map<String, Object>> records = new ArrayList<>();
        boolean includeAll = !hasText(type) || "all".equalsIgnoreCase(type);
        if (includeAll || "scenic".equalsIgnoreCase(type)) {
            records.addAll(getScenicCollectionRows(userIds, keyword));
        }
        if (includeAll || "guide".equalsIgnoreCase(type)) {
            records.addAll(getGuideCollectionRows(userIds, keyword));
        }
        if (includeAll || "tour".equalsIgnoreCase(type)) {
            records.addAll(getTourCollectionRows(userIds, keyword));
        }

        fillUnifiedUserInfo(records);
        records.sort((left, right) -> compareCreateTimeDesc(left, right));

        Page<Map<String, Object>> page = new Page<>(currentPage, size);
        page.setTotal(records.size());
        int from = Math.max(0, (currentPage - 1) * size);
        if (from >= records.size()) {
            page.setRecords(List.of());
        } else {
            int to = Math.min(records.size(), from + size);
            page.setRecords(records.subList(from, to));
        }
        return page;
    }

    public void deleteUnifiedCollection(String type, Long id) {
        int affected;
        if ("scenic".equalsIgnoreCase(type)) {
            affected = scenicCollectionMapper.deleteById(id);
        } else if ("guide".equalsIgnoreCase(type)) {
            affected = collectionMapper.deleteById(id);
        } else if ("tour".equalsIgnoreCase(type)) {
            affected = tourCollectionMapper.deleteById(id);
        } else {
            throw new ServiceException("收藏类型无效");
        }
        if (affected <= 0) {
            throw new ServiceException("收藏记录不存在或已被删除");
        }
    }

    private List<Map<String, Object>> getScenicCollectionRows(List<Long> userIds, String keyword) {
        LambdaQueryWrapper<ScenicCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(userIds != null && !userIds.isEmpty(), ScenicCollection::getUserId, userIds)
                .orderByDesc(ScenicCollection::getCreateTime);
        List<ScenicCollection> collections = scenicCollectionMapper.selectList(wrapper);
        if (collections.isEmpty()) {
            return List.of();
        }
        Map<Long, ScenicSpot> targetMap = scenicSpotMapper.selectBatchIds(collections.stream()
                        .map(ScenicCollection::getScenicId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ScenicSpot::getId, item -> item));
        return collections.stream()
                .map(item -> {
                    ScenicSpot scenic = targetMap.get(item.getScenicId());
                    return buildCollectionRow(item.getId(), "scenic", "景点收藏", item.getUserId(),
                            item.getScenicId(), scenic == null ? "已删除景点" : scenic.getName(),
                            scenic == null ? null : scenic.getImageUrl(), null, item.getCreateTime());
                })
                .filter(row -> matchKeyword(row, keyword))
                .toList();
    }

    private List<Map<String, Object>> getGuideCollectionRows(List<Long> userIds, String keyword) {
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(userIds != null && !userIds.isEmpty(), Collection::getUserId, userIds)
                .orderByDesc(Collection::getCreateTime);
        List<Collection> collections = collectionMapper.selectList(wrapper);
        if (collections.isEmpty()) {
            return List.of();
        }
        Map<Long, TravelGuide> targetMap = travelGuideMapper.selectBatchIds(collections.stream()
                        .map(Collection::getGuideId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(TravelGuide::getId, item -> item));
        return collections.stream()
                .map(item -> {
                    TravelGuide guide = targetMap.get(item.getGuideId());
                    return buildCollectionRow(item.getId(), "guide", "攻略收藏", item.getUserId(),
                            item.getGuideId(), guide == null ? "已删除攻略" : guide.getTitle(),
                            guide == null ? null : guide.getCoverImage(), guide == null ? null : guide.getViews(),
                            toLocalDateTime(item.getCreateTime()));
                })
                .filter(row -> matchKeyword(row, keyword))
                .toList();
    }

    private List<Map<String, Object>> getTourCollectionRows(List<Long> userIds, String keyword) {
        LambdaQueryWrapper<TourCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(userIds != null && !userIds.isEmpty(), TourCollection::getUserId, userIds)
                .orderByDesc(TourCollection::getCreateTime);
        List<TourCollection> collections = tourCollectionMapper.selectList(wrapper);
        if (collections.isEmpty()) {
            return List.of();
        }
        Map<Long, Tour> targetMap = tourMapper.selectBatchIds(collections.stream()
                        .map(TourCollection::getTourId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Tour::getId, item -> item));
        return collections.stream()
                .map(item -> {
                    Tour tour = targetMap.get(item.getTourId());
                    return buildCollectionRow(item.getId(), "tour", "行程收藏", item.getUserId(),
                            item.getTourId(), tour == null ? "已删除行程" : tour.getTitle(),
                            tour == null ? null : tour.getMainImage(), null, item.getCreateTime());
                })
                .filter(row -> matchKeyword(row, keyword))
                .toList();
    }

    private Map<String, Object> buildCollectionRow(Long id, String type, String typeLabel, Long userId, Long targetId,
                                                   String targetTitle, String coverImage, Integer views,
                                                   LocalDateTime createTime) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("type", type);
        row.put("typeLabel", typeLabel);
        row.put("userId", userId);
        row.put("targetId", targetId);
        row.put("targetTitle", targetTitle);
        row.put("coverImage", coverImage);
        row.put("views", views);
        row.put("createTime", createTime);
        return row;
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        Object title = row.get("targetTitle");
        return title != null && title.toString().contains(keyword);
    }

    private void fillUnifiedUserInfo(List<Map<String, Object>> records) {
        List<Long> userIds = records.stream()
                .map(row -> (Long) row.get("userId"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, item -> item));
        for (Map<String, Object> row : records) {
            User user = userMap.get(row.get("userId"));
            if (user != null) {
                row.put("username", user.getUsername());
                row.put("userNickname", user.getNickname());
                row.put("userAvatar", user.getAvatar());
            }
        }
    }

    private List<Long> resolveUserIds(String username) {
        if (!hasText(username)) {
            return List.of();
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(query -> query.like(User::getUsername, username).or().like(User::getNickname, username));
        return userMapper.selectList(wrapper).stream().map(User::getId).toList();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Page<Map<String, Object>> emptyUnifiedPage(Integer currentPage, Integer size) {
        Page<Map<String, Object>> page = new Page<>(currentPage, size);
        page.setRecords(List.of());
        page.setTotal(0);
        return page;
    }

    private int compareCreateTimeDesc(Map<String, Object> left, Map<String, Object> right) {
        LocalDateTime leftTime = (LocalDateTime) left.get("createTime");
        LocalDateTime rightTime = (LocalDateTime) right.get("createTime");
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        return rightTime.compareTo(leftTime);
    }
    
    /**
     * 填充攻略信息
     */
    private void fillGuideInfo(List<Collection> collections) {
        if (collections.isEmpty()) return;
        
        List<Long> guideIds = collections.stream()
                               .map(Collection::getGuideId)
                               .distinct()
                               .collect(Collectors.toList());
        
        if (!guideIds.isEmpty()) {
            Map<Long, TravelGuide> guideMap = travelGuideMapper.selectBatchIds(guideIds)
                                           .stream()
                                           .collect(Collectors.toMap(TravelGuide::getId, guide -> guide));
            
            for (Collection collection : collections) {
                TravelGuide guide = guideMap.get(collection.getGuideId());
                if (guide != null) {
                    collection.setGuideTitle(guide.getTitle());
                    collection.setGuideCoverImage(guide.getCoverImage());
                    collection.setGuideViews(guide.getViews());
                }
            }
        }
    }
    
    /**
     * 填充攻略和用户信息
     */
    private void fillGuideAndUserInfo(List<Collection> collections) {
        // 填充攻略信息
        fillGuideInfo(collections);
        
        // 填充用户信息
        if (!collections.isEmpty()) {
            List<Long> userIds = collections.stream()
                                 .map(Collection::getUserId)
                                 .distinct()
                                 .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                                         .stream()
                                         .collect(Collectors.toMap(User::getId, user -> user));
                
                for (Collection collection : collections) {
                    User user = userMap.get(collection.getUserId());
                    if (user != null) {
                        collection.setUsername(user.getUsername());
                        collection.setUserNickname(user.getNickname());
                        collection.setUserAvatar(user.getAvatar());
                    }
                }
            }
        }
    }
} 
