package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统操作日志实体类
 * 用于满足公安备案要求的操作审计日志留存
 */
@Data
@TableName("sys_operation_log")
@Schema(description = "系统操作日志实体类")
public class SysOperationLog {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;
    
    @Schema(description = "操作用户ID")
    private Long userId;
    
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "操作人角色编码: SUPER_ADMIN/ADMIN/USER")
    private String roleCode;

    @Schema(description = "操作人角色名称")
    private String roleName;
    
    @Schema(description = "操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/QUERY")
    private String operationType;

    @Schema(description = "日志等级: INFO/WARN/ERROR")
    private String logLevel;
    
    @Schema(description = "操作描述")
    private String operationDesc;
    
    @Schema(description = "操作对象类型")
    private String targetType;
    
    @Schema(description = "操作对象ID")
    private String targetId;
    
    @Schema(description = "请求URL")
    private String requestUrl;
    
    @Schema(description = "请求方法")
    private String requestMethod;
    
    @Schema(description = "请求参数")
    private String requestParams;
    
    @Schema(description = "IP地址")
    private String ipAddress;
    
    @Schema(description = "端口")
    private Integer port;
    
    @Schema(description = "浏览器信息")
    private String userAgent;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备指纹")
    private String deviceFingerprint;

    @Schema(description = "客户端硬件特征")
    private String clientHardware;

    @Schema(description = "MAC地址")
    private String macAddress;
    
    @Schema(description = "响应状态: 1-成功, 0-失败")
    private Integer status;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "执行时长(毫秒)")
    private Long executionTime;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
