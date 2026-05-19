package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sensitive_word")
@Schema(description = "敏感词规则")
public class SensitiveWord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "敏感词")
    private String word;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "处置级别: BLOCK-拦截 REVIEW-脱敏后人工审核 REPLACE-脱敏")
    private String level;

    @Schema(description = "匹配方式: CONTAINS-包含 EXACT-完全匹配")
    private String matchType;

    @Schema(description = "替换文本")
    private String replacement;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态: 1-启用 0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
