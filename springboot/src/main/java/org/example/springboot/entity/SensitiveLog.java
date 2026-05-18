package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词检测日志实体
 */
@Data
@TableName("sensitive_log")
@Schema(description = "敏感词检测日志实体")
public class SensitiveLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "检测内容")
    private String content;

    @Schema(description = "内容类型: COMMENT-评论 GUIDE-游记 DESCRIPTION-描述等")
    private String contentType;

    @Schema(description = "关联对象ID")
    private String objectId;

    @Schema(description = "检测到的敏感词(JSON数组)")
    private String detectedWords;

    @Schema(description = "敏感词数量")
    private Integer detectedCount;

    @Schema(description = "处理方式: REJECT-拒绝 REPLACE-替换 WARN-警告 PASS-放行")
    private String handleType;

    @Schema(description = "处理结果(替换后的内容等)")
    private String handleResult;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "检测时间")
    private LocalDateTime createTime;

    @Schema(description = "状态: 0-已删除 1-正常")
    private Integer status;
}
