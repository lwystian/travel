package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_moderation_config")
@Schema(description = "内容审核策略配置")
public class ContentModerationConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置说明")
    private String description;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
