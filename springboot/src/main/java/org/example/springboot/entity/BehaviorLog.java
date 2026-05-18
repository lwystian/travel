package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为日志实体
 */
@Data
@TableName("behavior_log")
@Schema(description = "用户行为日志实体")
public class BehaviorLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "行为类型")
    private String behaviorType;

    @Schema(description = "行为名称")
    private String behaviorName;

    @Schema(description = "页面URL")
    private String pageUrl;

    @Schema(description = "页面名称")
    private String pageName;

    @Schema(description = "触发元素ID")
    private String elementId;

    @Schema(description = "触发元素名称")
    private String elementName;

    @Schema(description = "来源页面")
    private String referrer;

    @Schema(description = "页面停留时长(秒)")
    private Integer stayDuration;

    @Schema(description = "行为数据(JSON)")
    private String actionData;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "屏幕分辨率")
    private String screenSize;

    @Schema(description = "行为时间")
    private LocalDateTime createTime;

    @Schema(description = "状态: 0-已删除 1-正常")
    private Integer status;
}
