package org.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.ReviewLog;
import org.example.springboot.mapper.ReviewLogMapper;
import org.example.springboot.security.SecurityValidationUtil;
import org.example.springboot.service.ReviewLogService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewLogServiceImpl extends ServiceImpl<ReviewLogMapper, ReviewLog> implements ReviewLogService {
    @Override
    public void recordReview(Long userId, String username, String nickname, String targetType, String targetId,
                            Long targetUserId, String targetSummary, String contentPreview, String reviewType,
                            String reviewStatus, String previousStatus, String rejectReason, String reviewTags,
                            BigDecimal aiScore, String ip, String userAgent) {
        ReviewLog log = new ReviewLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setNickname(nickname);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetUserId(targetUserId);
        log.setTargetSummary(targetSummary);
        log.setContentPreview(contentPreview);
        log.setReviewType(reviewType);
        log.setReviewStatus(reviewStatus);
        log.setPreviousStatus(previousStatus);
        log.setRejectReason(rejectReason);
        log.setReviewTags(reviewTags);
        log.setAiScore(aiScore);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setCreateTime(LocalDateTime.now());
        log.setStatus(1);
        this.save(log);
    }

    @Override
    public List<ReviewLog> queryLogs(int page, int limit, String username, LocalDateTime startTime, LocalDateTime endTime) {
        page = SecurityValidationUtil.clampPage(page);
        limit = SecurityValidationUtil.clampLimit(limit, 10, 100);
        LambdaQueryWrapper<ReviewLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), ReviewLog::getUsername, username);
        wrapper.ge(startTime != null, ReviewLog::getCreateTime, startTime);
        wrapper.le(endTime != null, ReviewLog::getCreateTime, endTime);
        wrapper.orderByDesc(ReviewLog::getCreateTime);
        wrapper.last("LIMIT " + ((page - 1) * limit) + ", " + limit);
        return this.list(wrapper);
    }

    @Override
    public long countLogs(String username, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ReviewLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), ReviewLog::getUsername, username);
        wrapper.ge(startTime != null, ReviewLog::getCreateTime, startTime);
        wrapper.le(endTime != null, ReviewLog::getCreateTime, endTime);
        return this.count(wrapper);
    }
}
