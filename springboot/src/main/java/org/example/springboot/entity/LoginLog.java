package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
@TableName("login_log")
@Schema(description = "登录日志实体")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "登录类型: NORMAL-正常登录 TOKEN-refresh_token登录 WECHAT-微信登录")
    private String loginType;

    @Schema(description = "登录状态: SUCCESS-成功 FAIL-失败")
    private String loginStatus;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "端口")
    private Integer port;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录地点")
    private String location;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "Token(脱敏)")
    private String token;

    @Schema(description = "登出时间")
    private LocalDateTime logoutTime;

    @Schema(description = "登录时间")
    private LocalDateTime createTime;

    @Schema(description = "状态: 0-已删除 1-正常")
    private Integer status;
}
