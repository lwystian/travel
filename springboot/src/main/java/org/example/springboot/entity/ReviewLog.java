package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内容审核日志实体
 */
@Data
@TableName("review_log")
@Schema(description = "内容审核日志实体")
public class ReviewLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作用户ID(审核人)")
    private Long userId;

    @Schema(description = "操作用户名")
    private String username;

    @Schema(description = "操作人昵称")
    private String nickname;

    @Schema(description = "审核对象类型: COMMENT-评论 GUIDE-游记 IMAGE-图片 USER-用户")
    private String targetType;

    @Schema(description = "审核对象ID")
    private String targetId;

    @Schema(description = "被审核内容的用户ID")
    private Long targetUserId;

    @Schema(description = "被审核内容摘要")
    private String targetSummary;

    @Schema(description = "内容预览(前500字符)")
    private String contentPreview;

    @Schema(description = "审核类型: AUTO-自动 MANUAL-人工")
    private String reviewType;

    @Schema(description = "审核结果: PENDING-待审核 APPROVED-通过 REJECTED-拒绝")
    private String reviewStatus;

    @Schema(description = "原状态")
    private String previousStatus;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "审核标签(JSON数组): 涉黄/涉暴/广告等")
    private String reviewTags;

    @Schema(description = "AI审核分数(0-1)")
    private BigDecimal aiScore;

    @Schema(description = "审核人IP")
    private String ip;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "审核时间")
    private LocalDateTime createTime;

    @Schema(description = "状态: 0-已删除 1-正常")
    private Integer status;
}
