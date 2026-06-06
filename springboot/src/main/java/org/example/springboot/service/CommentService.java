package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.CommentLike;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CommentLikeMapper;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class CommentService {
    @Resource
    private CommentMapper commentMapper;
    
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    
    @Resource
    private UserMapper userMapper;
    @Resource
    private CommentLikeMapper commentLikeMapper;
    @Resource
    private SensitiveWordService sensitiveWordService;
    @Resource
    private ContentModerationConfigService contentModerationConfigService;

    public Page<Comment> getCommentsByPage(Long scenicId, String scenicName, String userName, String content, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        
        // 只查询已审核通过的评论
        queryWrapper.eq(Comment::getReviewStatus, 1);
        
        // 如果提供了景点ID，直接使用ID查询
        if (scenicId != null) {
            queryWrapper.eq(Comment::getScenicId, scenicId);
        } 
        // 如果提供了景点名称，先查询景点ID，再使用ID查询评论
        else if (StringUtils.isNotBlank(scenicName)) {
            List<Long> scenicIds = getScenicIdsByName(scenicName);
            if (scenicIds.isEmpty()) {
                // 如果没有找到匹配的景点，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Comment::getScenicId, scenicIds);
        }
        
        // 如果提供了用户名/昵称，先查询用户ID，再使用ID查询评论
        if (StringUtils.isNotBlank(userName)) {
            List<Long> userIds = getUserIdsByName(userName);
            if (userIds.isEmpty()) {
                // 如果没有找到匹配的用户，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Comment::getUserId, userIds);
        }
        
        // 如果提供了内容关键词，按内容过滤
        if (StringUtils.isNotBlank(content)) {
            queryWrapper.like(Comment::getContent, content);
        }
        
        queryWrapper.orderByDesc(Comment::getCreateTime);
        return commentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    private List<Long> getScenicIdsByName(String name) {
        if (StringUtils.isBlank(name)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(ScenicSpot::getName, name);
        return scenicSpotMapper.selectList(queryWrapper)
            .stream()
            .map(ScenicSpot::getId)
            .toList();
    }
    
    private List<Long> getUserIdsByName(String name) {
        if (StringUtils.isBlank(name)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(User::getUsername, name)
                .or()
                .like(User::getNickname, name));
        return userMapper.selectList(queryWrapper)
            .stream()
            .map(User::getId)
            .toList();
    }

    public void addComment(Comment comment) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new ServiceException("请先登录");
        }
        comment.setUserId(currentUser.getId());
        applyCommentModeration(comment, currentUser);
        if (commentMapper.insert(comment) <= 0) throw new ServiceException("评论失败");
    }

    private void applyCommentModeration(Comment comment, User currentUser) {
        String roleCode = RolePermission.normalizeRole(currentUser.getRoleCode());
        if (RolePermission.SUPER_ADMIN.equals(roleCode)) {
            comment.setReviewStatus(ContentReviewService.STATUS_APPROVED);
            return;
        }
        if (RolePermission.ADMIN.equals(roleCode)) {
            comment.setReviewStatus(contentModerationConfigService.adminCommentReviewRequired()
                    ? ContentReviewService.STATUS_PENDING
                    : ContentReviewService.STATUS_APPROVED);
            return;
        }
        comment.setContent(sensitiveWordService.filterContent(comment.getContent(), "COMMENT", null));
        comment.setReviewStatus(ContentReviewService.STATUS_PENDING);
    }

    @Transactional
    public void deleteComment(Long id, Long userId, boolean isAdmin) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new ServiceException("评论不存在");
        if (!isAdmin && !comment.getUserId().equals(userId)) throw new ServiceException("无权删除");
        deleteCommentLikes(id);
        if (commentMapper.deleteById(id) <= 0) throw new ServiceException("删除失败");
    }

    public void deleteCommentLikes(Long commentId) {
        commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId));
    }

    public void likeComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new ServiceException("评论不存在");
        // 只允许点赞已审核通过的评论
        if (comment.getReviewStatus() == null || comment.getReviewStatus() != 1) {
            throw new ServiceException("该评论未通过审核，无法点赞");
        }
        comment.setLikes(comment.getLikes() == null ? 1 : comment.getLikes() + 1);
        if (commentMapper.updateById(comment) <= 0) throw new ServiceException("点赞失败");
    }

    public Comment getById(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new ServiceException("评论不存在");
        return comment;
    }

    public List<Comment> getAllByScenicId(Long scenicId) {
        // 只查询已审核通过的评论
        return commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getScenicId, scenicId)
                .eq(Comment::getReviewStatus, 1)
                .orderByDesc(Comment::getCreateTime)
        );
    }

    public Page<Comment> getMyComments(Long userId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getUserId, userId);
        queryWrapper.orderByDesc(Comment::getCreateTime);
        return commentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
} 
