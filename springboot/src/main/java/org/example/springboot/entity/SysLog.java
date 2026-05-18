package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统操作日志实体
 */
@Data
@TableName("sys_log")
@Schema(description = "系统操作日志实体")
public class SysLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "操作类型")
    private String operation;

    @Schema(description = "操作名称")
    private String operationName;

    @Schema(description = "操作对象类型")
    private String objectType;

    @Schema(description = "操作对象ID")
    private String objectId;

    @Schema(description = "操作对象名称")
    private String objectName;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求URL")
    private String requestUrl;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "请求体")
    private String requestBody;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "端口")
    private Integer port;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "操作结果: SUCCESS/FAIL/ERROR")
    private String result;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "响应数据(脱敏)")
    private String responseData;

    @Schema(description = "操作耗时(毫秒)")
    private Integer duration;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;

    @Schema(description = "状态: 0-已删除 1-正常")
    private Integer status;
}
