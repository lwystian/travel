package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.TravelGuideMapper;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private TravelGuideMapper travelGuideMapper;
    
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
        
        stats.put("approvedCommentCount", commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getReviewStatus, STATUS_APPROVED)));
        stats.put("rejectedCommentCount", commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getReviewStatus, STATUS_REJECTED)));
        
        stats.put("approvedGuideCount", travelGuideMapper.selectCount(
                new LambdaQueryWrapper<TravelGuide>().eq(TravelGuide::getReviewStatus, STATUS_APPROVED)));
        stats.put("rejectedGuideCount", travelGuideMapper.selectCount(
                new LambdaQueryWrapper<TravelGuide>().eq(TravelGuide::getReviewStatus, STATUS_REJECTED)));
        
        return stats;
    }
    
    // ==================== 私有方法 ====================

    @Resource
    private UserMapper userMapper;

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
}
