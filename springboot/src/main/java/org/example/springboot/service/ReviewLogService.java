package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.springboot.entity.ReviewLog;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewLogService extends IService<ReviewLog> {
    void recordReview(Long userId, String username, String nickname, String targetType, String targetId,
                     Long targetUserId, String targetSummary, String contentPreview, String reviewType,
                     String reviewStatus, String previousStatus, String rejectReason, String reviewTags,
                     BigDecimal aiScore, String ip, String userAgent);
    List<ReviewLog> queryLogs(int page, int limit, String username, LocalDateTime startTime, LocalDateTime endTime);
    long countLogs(String username, LocalDateTime startTime, LocalDateTime endTime);
}
