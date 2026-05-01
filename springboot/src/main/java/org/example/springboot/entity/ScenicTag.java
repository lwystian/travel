package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scenic_tag")
@Schema(description = "景点标签实体类")
public class ScenicTag {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "标签ID")
    private Long id;
    
    @Schema(description = "标签名称")
    private String name;
    
    @Schema(description = "标签颜色")
    private String color;
    
    @Schema(description = "标签图标")
    private String icon;
    
    @Schema(description = "排序序号")
    private Integer sortOrder;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
