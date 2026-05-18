package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据备份记录实体
 */
@Data
@TableName("backup_log")
@Schema(description = "数据备份记录实体")
public class BackupLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "备份ID")
    private Long id;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人用户名")
    private String operatorName;

    @Schema(description = "备份类型: LOG-日志 DB-数据库 FILE-文件")
    private String backupType;

    @Schema(description = "备份的表名")
    private String backupTable;

    @Schema(description = "备份文件名称")
    private String backupName;

    @Schema(description = "备份文件路径")
    private String backupPath;

    @Schema(description = "备份文件大小(字节)")
    private Long backupSize;

    @Schema(description = "备份条件(Where子句)")
    private String backupCondition;

    @Schema(description = "备份记录数")
    private Integer recordCount;

    @Schema(description = "备份时间范围开始")
    private LocalDateTime timeRangeStart;

    @Schema(description = "备份时间范围结束")
    private LocalDateTime timeRangeEnd;

    @Schema(description = "备份状态: SUCCESS-成功 FAIL-失败 RUNNING-进行中")
    private String status;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "下载URL")
    private String fileUrl;

    @Schema(description = "备份时间")
    private LocalDateTime createTime;

    @Schema(description = "过期时间(默认6个月后)")
    private LocalDateTime expireTime;

    @Schema(description = "状态: 0-已删除 1-正常")
    private Integer statusFlag;
}
