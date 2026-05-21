package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.AccommodationReview;
import org.example.springboot.entity.Accommodation;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AccommodationReviewMapper;
import org.example.springboot.mapper.AccommodationMapper;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.TravelGuideMapper;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 内容审核服务
 * 统一管理评论和攻略的审核流程
 */
@Service
public class ContentReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentReviewService.class);
    
    // 审核状态常量
    public static final int STATUS_PENDING = 0;   // 待审核
    public static final int STATUS_APPROVED = 1; // 已通过
    public static final int STATUS_REJECTED = 2;  // 已拒绝
    
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private AccommodationReviewMapper accommodationReviewMapper;
    @Resource
    private AccommodationMapper accommodationMapper;
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    
    @Resource
    private TravelGuideMapper travelGuideMapper;
    @Resource
    private SiteNotificationService siteNotificationService;
    
    // ==================== 评论审核 ====================
    
    /**
     * 分页查询待审核评论
     */
    public Page<Comment> getPendingComments(String scenicName, String userName, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getReviewStatus, STATUS_PENDING);
        
        if (scenicName != null && !scenicName.isEmpty()) {
            queryWrapper.like(Comment::getContent, scenicName);
        }
        
        queryWrapper.orderByAsc(Comment::getCreateTime); // 按时间正序，先提交的先审核
        
        Page<Comment> page = commentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充用户信息
        fillCommentUserInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 分页查询所有评论（管理员）
     */
    public Page<Comment> getAllComments(String scenicName, String userName, Integer reviewStatus, 
            Integer currentPage, Integer size) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        
        if (reviewStatus != null) {
            queryWrapper.eq(Comment::getReviewStatus, reviewStatus);
        }
        
        if (scenicName != null && !scenicName.isEmpty()) {
            queryWrapper.like(Comment::getContent, scenicName);
        }
        
        queryWrapper.orderByDesc(Comment::getCreateTime);
        
        Page<Comment> page = commentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        fillCommentUserInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 审核评论
     */
    @Transactional
    public void reviewComment(Long commentId, Integer status, Long reviewerId, String reviewerName, String comment) {
        Comment commentEntity = commentMapper.selectById(commentId);
        if (commentEntity == null) {
            throw new ServiceException("评论不存在");
        }
        
        if (commentEntity.getReviewStatus() != null && commentEntity.getReviewStatus() != STATUS_PENDING) {
            throw new ServiceException("该评论已审核过，无法重复审核");
        }
        
        if (status != STATUS_APPROVED && status != STATUS_REJECTED) {
            throw new ServiceException("审核状态无效");
        }
        
        commentEntity.setReviewStatus(status);
        commentEntity.setReviewerId(reviewerId);
        commentEntity.setReviewerName(reviewerName);
        commentEntity.setReviewTime(LocalDateTime.now());
        commentEntity.setReviewComment(comment);
        
        commentMapper.updateById(commentEntity);
        siteNotificationService.sendToUser(
                commentEntity.getUserId(),
                status == STATUS_APPROVED ? "评论审核通过" : "评论审核未通过",
                status == STATUS_APPROVED ? "你的评论审核通过，已在评论区展示。" : "你的评论未通过审核，原因：" + (comment == null ? "内容不符合平台规范" : comment),
                "REVIEW",
                "COMMENT",
                String.valueOf(commentId),
                "/profile"
        );
        logger.info("评论审核完成，评论ID: {}, 审核结果: {}, 审核人: {}", 
                commentId, status == STATUS_APPROVED ? "通过" : "拒绝", reviewerName);
    }
    
    /**
     * 获取待审核评论数量
     */
    public long getPendingCommentCount() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getReviewStatus, STATUS_PENDING));
    }

    // ==================== 住宿评价审核 ====================

    public Page<AccommodationReview> getPendingAccommodationReviews(Integer currentPage, Integer size) {
        LambdaQueryWrapper<AccommodationReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AccommodationReview::getReviewStatus, STATUS_PENDING)
                .orderByAsc(AccommodationReview::getCreateTime);
        Page<AccommodationReview> page = accommodationReviewMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        fillAccommodationReviewUserInfo(page.getRecords());
        return page;
    }

    public Page<AccommodationReview> getAllAccommodationReviews(Integer reviewStatus, Integer currentPage, Integer size) {
        LambdaQueryWrapper<AccommodationReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(reviewStatus != null, AccommodationReview::getReviewStatus, reviewStatus)
                .orderByDesc(AccommodationReview::getCreateTime);
        Page<AccommodationReview> page = accommodationReviewMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        fillAccommodationReviewUserInfo(page.getRecords());
        return page;
    }

    @Transactional
    public void reviewAccommodationReview(Long reviewId, Integer status, Long reviewerId, String reviewerName, String comment) {
        AccommodationReview review = accommodationReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new ServiceException("住宿评价不存在");
        }
        if (review.getReviewStatus() != null && review.getReviewStatus() != STATUS_PENDING) {
            throw new ServiceException("该住宿评价已审核过，无法重复审核");
        }
        if (status != STATUS_APPROVED && status != STATUS_REJECTED) {
            throw new ServiceException("审核状态无效");
        }

        review.setReviewStatus(status);
        review.setReviewerId(reviewerId);
        review.setReviewerName(reviewerName);
        review.setReviewTime(LocalDateTime.now());
        review.setReviewComment(comment);
        accommodationReviewMapper.updateById(review);
        if (status == STATUS_APPROVED) {
            updateAccommodationRating(review.getAccommodationId());
        }

        siteNotificationService.sendToUser(
                review.getUserId(),
                status == STATUS_APPROVED ? "住宿评价审核通过" : "住宿评价审核未通过",
                status == STATUS_APPROVED ? "你的住宿评价审核通过，已在评论区展示。" : "你的住宿评价未通过审核，原因：" + (comment == null ? "内容不符合平台规范" : comment),
                "REVIEW",
                "ACCOMMODATION_REVIEW",
                String.valueOf(reviewId),
                review.getAccommodationId() == null ? "/accommodation" : "/accommodation/" + review.getAccommodationId()
        );
        logger.info("住宿评价审核完成，评价ID: {}, 审核结果: {}, 审核人: {}",
                reviewId, status == STATUS_APPROVED ? "通过" : "拒绝", reviewerName);
    }

    public long getPendingAccommodationReviewCount() {
        return accommodationReviewMapper.selectCount(new LambdaQueryWrapper<AccommodationReview>()
                .eq(AccommodationReview::getReviewStatus, STATUS_PENDING));
    }

    public Page<Map<String, Object>> getUnifiedReviewPage(String type, String targetName, String userName,
                                                          String content, Integer reviewStatus,
                                                          Integer currentPage, Integer size) {
        List<Long> userIds = resolveUserIds(userName);
        if (hasText(userName) && userIds.isEmpty()) {
            return emptyMapPage(currentPage, size);
        }

        List<Map<String, Object>> records = new ArrayList<>();
        boolean includeAll = !hasText(type) || "all".equalsIgnoreCase(type);
        if (includeAll || "scenic".equalsIgnoreCase(type)) {
            records.addAll(getScenicReviewRows(userIds, targetName, content, reviewStatus));
        }
        if (includeAll || "accommodation".equalsIgnoreCase(type)) {
            records.addAll(getAccommodationReviewRows(userIds, targetName, content, reviewStatus));
        }
        records.sort((left, right) -> compareCreateTimeDesc(left, right));

        Page<Map<String, Object>> page = new Page<>(currentPage, size);
        page.setTotal(records.size());
        int from = Math.max(0, (currentPage - 1) * size);
        if (from >= records.size()) {
            page.setRecords(List.of());
        } else {
            page.setRecords(records.subList(from, Math.min(records.size(), from + size)));
        }
        return page;
    }

    @Transactional
    public void deleteUnifiedReview(String type, Long id) {
        if ("scenic".equalsIgnoreCase(type)) {
            if (commentMapper.deleteById(id) <= 0) {
                throw new ServiceException("景点评论不存在或已被删除");
            }
            return;
        }
        if ("accommodation".equalsIgnoreCase(type)) {
            AccommodationReview review = accommodationReviewMapper.selectById(id);
            if (review == null) {
                throw new ServiceException("住宿评价不存在或已被删除");
            }
            if (accommodationReviewMapper.deleteById(id) <= 0) {
                throw new ServiceException("住宿评价删除失败");
            }
            updateAccommodationRating(review.getAccommodationId());
            return;
        }
        throw new ServiceException("评论类型无效");
    }
    
    // ==================== 攻略审核 ====================
    
    /**
     * 分页查询待审核攻略
     */
    public Page<TravelGuide> getPendingGuides(String title, String userName, Integer currentPage, Integer size) {
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TravelGuide::getReviewStatus, STATUS_PENDING);
        
        if (title != null && !title.isEmpty()) {
            queryWrapper.like(TravelGuide::getTitle, title);
        }
        
        queryWrapper.orderByAsc(TravelGuide::getCreateTime);
        
        Page<TravelGuide> page = travelGuideMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        fillGuideUserInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 分页查询所有攻略（管理员）
     */
    public Page<TravelGuide> getAllGuides(String title, Integer reviewStatus, 
            Integer currentPage, Integer size) {
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        
        if (reviewStatus != null) {
            queryWrapper.eq(TravelGuide::getReviewStatus, reviewStatus);
        }
        
        if (title != null && !title.isEmpty()) {
            queryWrapper.like(TravelGuide::getTitle, title);
        }
        
        queryWrapper.orderByDesc(TravelGuide::getCreateTime);
        
        Page<TravelGuide> page = travelGuideMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        fillGuideUserInfo(page.getRecords());
        
        return page;
    }
    
    /**
     * 审核攻略
     */
    @Transactional
    public void reviewGuide(Long guideId, Integer status, Long reviewerId, String reviewerName, String comment) {
        TravelGuide guide = travelGuideMapper.selectById(guideId);
        if (guide == null) {
            throw new ServiceException("攻略不存在");
        }
        
        if (guide.getReviewStatus() != null && guide.getReviewStatus() != STATUS_PENDING) {
            throw new ServiceException("该攻略已审核过，无法重复审核");
        }
        
        if (status != STATUS_APPROVED && status != STATUS_REJECTED) {
            throw new ServiceException("审核状态无效");
        }
        
        guide.setReviewStatus(status);
        guide.setReviewerId(reviewerId);
        guide.setReviewerName(reviewerName);
        guide.setReviewTime(LocalDateTime.now());
        guide.setReviewComment(comment);
        
        travelGuideMapper.updateById(guide);
        siteNotificationService.sendToUser(
                guide.getUserId(),
                status == STATUS_APPROVED ? "攻略审核通过" : "攻略审核未通过",
                status == STATUS_APPROVED ? "你的攻略《" + guide.getTitle() + "》已审核通过。" : "你的攻略《" + guide.getTitle() + "》未通过审核，原因：" + (comment == null ? "内容不符合平台规范" : comment),
                "REVIEW",
                "GUIDE",
                String.valueOf(guideId),
                "/my-guide"
        );
        logger.info("攻略审核完成，攻略ID: {}, 审核结果: {}, 审核人: {}", 
                guideId, status == STATUS_APPROVED ? "通过" : "拒绝", reviewerName);
    }
    
    /**
     * 获取待审核攻略数量
     */
    public long getPendingGuideCount() {
        return travelGuideMapper.selectCount(new LambdaQueryWrapper<TravelGuide>()
                .eq(TravelGuide::getReviewStatus, STATUS_PENDING));
    }
    
    // ==================== 统计 ====================
    
    /**
     * 获取审核统计信息
     */
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("pendingCommentCount", getPendingCommentCount());
        stats.put("pendingGuideCount", getPendingGuideCount());
        stats.put("pendingAccommodationReviewCount", getPendingAccommodationReviewCount());
        
        stats.put("approvedCommentCount", commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getReviewStatus, STATUS_APPROVED)));
        stats.put("rejectedCommentCount", commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getReviewStatus, STATUS_REJECTED)));
        stats.put("approvedAccommodationReviewCount", accommodationReviewMapper.selectCount(
                new LambdaQueryWrapper<AccommodationReview>().eq(AccommodationReview::getReviewStatus, STATUS_APPROVED)));
        stats.put("rejectedAccommodationReviewCount", accommodationReviewMapper.selectCount(
                new LambdaQueryWrapper<AccommodationReview>().eq(AccommodationReview::getReviewStatus, STATUS_REJECTED)));
        
        stats.put("approvedGuideCount", travelGuideMapper.selectCount(
                new LambdaQueryWrapper<TravelGuide>().eq(TravelGuide::getReviewStatus, STATUS_APPROVED)));
        stats.put("rejectedGuideCount", travelGuideMapper.selectCount(
                new LambdaQueryWrapper<TravelGuide>().eq(TravelGuide::getReviewStatus, STATUS_REJECTED)));
        
        return stats;
    }
    
    // ==================== 私有方法 ====================

    @Resource
    private UserMapper userMapper;

    private List<Map<String, Object>> getScenicReviewRows(List<Long> userIds, String targetName,
                                                          String content, Integer reviewStatus) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(userIds != null && !userIds.isEmpty(), Comment::getUserId, userIds)
                .eq(reviewStatus != null, Comment::getReviewStatus, reviewStatus)
                .like(hasText(content), Comment::getContent, content)
                .orderByDesc(Comment::getCreateTime);
        List<Comment> comments = commentMapper.selectList(wrapper);
        if (comments.isEmpty()) {
            return List.of();
        }
        Map<Long, ScenicSpot> targetMap = scenicSpotMapper.selectBatchIds(comments.stream()
                        .map(Comment::getScenicId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(ScenicSpot::getId, item -> item));
        Map<Long, User> userMap = getUserMap(comments.stream().map(Comment::getUserId).toList());

        return comments.stream()
                .map(item -> {
                    ScenicSpot scenic = targetMap.get(item.getScenicId());
                    User user = userMap.get(item.getUserId());
                    return buildReviewRow(item.getId(), "scenic", "景点评论", item.getUserId(),
                            item.getScenicId(), scenic == null ? "已删除景点" : scenic.getName(),
                            item.getContent(), item.getRating(), item.getLikes(), item.getReviewStatus(),
                            item.getReviewerName(), item.getReviewTime(), item.getReviewComment(), item.getCreateTime(), user);
                })
                .filter(row -> matchTargetName(row, targetName))
                .toList();
    }

    private List<Map<String, Object>> getAccommodationReviewRows(List<Long> userIds, String targetName,
                                                                 String content, Integer reviewStatus) {
        LambdaQueryWrapper<AccommodationReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(userIds != null && !userIds.isEmpty(), AccommodationReview::getUserId, userIds)
                .eq(reviewStatus != null, AccommodationReview::getReviewStatus, reviewStatus)
                .like(hasText(content), AccommodationReview::getContent, content)
                .orderByDesc(AccommodationReview::getCreateTime);
        List<AccommodationReview> reviews = accommodationReviewMapper.selectList(wrapper);
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<Long, Accommodation> targetMap = accommodationMapper.selectBatchIds(reviews.stream()
                        .map(AccommodationReview::getAccommodationId)
                        .filter(Objects::nonNull)
                        .map(Integer::longValue)
                        .distinct()
                        .toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Accommodation::getId, item -> item));
        Map<Long, User> userMap = getUserMap(reviews.stream().map(AccommodationReview::getUserId).toList());

        return reviews.stream()
                .map(item -> {
                    Accommodation accommodation = item.getAccommodationId() == null
                            ? null
                            : targetMap.get(item.getAccommodationId().longValue());
                    return buildReviewRow(item.getId(), "accommodation", "住宿评价", item.getUserId(),
                            item.getAccommodationId() == null ? null : item.getAccommodationId().longValue(),
                            accommodation == null ? "已删除住宿" : accommodation.getName(),
                            item.getContent(), item.getRating(), null, item.getReviewStatus(),
                            item.getReviewerName(), item.getReviewTime(), item.getReviewComment(), item.getCreateTime(),
                            userMap.get(item.getUserId()));
                })
                .filter(row -> matchTargetName(row, targetName))
                .toList();
    }

    private Map<String, Object> buildReviewRow(Long id, String type, String typeLabel, Long userId, Long targetId,
                                               String targetName, String content, Object rating, Integer likes,
                                               Integer reviewStatus, String reviewerName, LocalDateTime reviewTime,
                                               String reviewComment, LocalDateTime createTime, User user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("type", type);
        row.put("typeLabel", typeLabel);
        row.put("userId", userId);
        row.put("targetId", targetId);
        row.put("targetName", targetName);
        row.put("content", content);
        row.put("rating", rating);
        row.put("likes", likes);
        row.put("reviewStatus", reviewStatus);
        row.put("reviewerName", reviewerName);
        row.put("reviewTime", reviewTime);
        row.put("reviewComment", reviewComment);
        row.put("createTime", createTime);
        if (user != null) {
            row.put("username", user.getUsername());
            row.put("userNickname", user.getNickname());
            row.put("userAvatar", user.getAvatar());
        }
        return row;
    }

    private boolean matchTargetName(Map<String, Object> row, String targetName) {
        if (!hasText(targetName)) {
            return true;
        }
        Object name = row.get("targetName");
        return name != null && name.toString().contains(targetName);
    }

    private List<Long> resolveUserIds(String userName) {
        if (!hasText(userName)) {
            return List.of();
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(query -> query.like(User::getUsername, userName).or().like(User::getNickname, userName));
        return userMapper.selectList(wrapper).stream().map(User::getId).toList();
    }

    private Map<Long, User> getUserMap(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, item -> item));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Page<Map<String, Object>> emptyMapPage(Integer currentPage, Integer size) {
        Page<Map<String, Object>> page = new Page<>(currentPage, size);
        page.setTotal(0);
        page.setRecords(List.of());
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

    private void fillCommentUserInfo(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        // 获取所有用户ID
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        // 批量查询用户信息
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        // 填充到评论
        for (Comment comment : comments) {
            if (comment.getUserId() != null && userMap.containsKey(comment.getUserId())) {
                User user = userMap.get(comment.getUserId());
                comment.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                comment.setUserAvatar(user.getAvatar());
            }
        }
    }

    private void fillGuideUserInfo(List<TravelGuide> guides) {
        if (guides == null || guides.isEmpty()) {
            return;
        }
        // 获取所有用户ID
        List<Long> userIds = guides.stream()
                .map(TravelGuide::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        // 批量查询用户信息
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        // 填充到攻略
        for (TravelGuide guide : guides) {
            if (guide.getUserId() != null && userMap.containsKey(guide.getUserId())) {
                User user = userMap.get(guide.getUserId());
                guide.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                guide.setUserAvatar(user.getAvatar());
            }
        }
    }

    private void fillAccommodationReviewUserInfo(List<AccommodationReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }
        List<Long> userIds = reviews.stream()
                .map(AccommodationReview::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        for (AccommodationReview review : reviews) {
            if (review.getUserId() != null && userMap.containsKey(review.getUserId())) {
                User user = userMap.get(review.getUserId());
                review.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                review.setAvatar(user.getAvatar());
            }
        }
    }

    private void updateAccommodationRating(Integer accommodationId) {
        if (accommodationId == null) {
            return;
        }
        List<AccommodationReview> reviews = accommodationReviewMapper.selectList(new LambdaQueryWrapper<AccommodationReview>()
                .eq(AccommodationReview::getAccommodationId, accommodationId)
                .eq(AccommodationReview::getReviewStatus, STATUS_APPROVED));
        if (reviews.isEmpty()) {
            return;
        }
        BigDecimal totalRating = BigDecimal.ZERO;
        for (AccommodationReview review : reviews) {
            if (review.getRating() != null) {
                totalRating = totalRating.add(review.getRating());
            }
        }
        BigDecimal averageRating = totalRating.divide(new BigDecimal(reviews.size()), 1, RoundingMode.HALF_UP);
        org.example.springboot.entity.Accommodation accommodation = accommodationMapper.selectById(accommodationId);
        if (accommodation != null) {
            accommodation.setStarLevel(averageRating);
            accommodationMapper.updateById(accommodation);
        }
    }
}
