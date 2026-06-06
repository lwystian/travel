package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scenic_spot")
@Schema(description = "景点实体类")
public class ScenicSpot {
    @TableId(type = IdType.AUTO)
    @Schema(description = "景点ID")
    private Long id;

    @Schema(description = "景点名称")
    private String name;

    @Schema(description = "景点描述")
    private String description;

    @Schema(description = "地理位置")
    private String location;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "开放时间")
    private String openingHours;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "标签，多个标签用空格分隔")
    private String tags;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "分类信息")
    private ScenicCategory categoryInfo;

    @TableField(exist = false)
    @Schema(description = "平均评分")
    private Double rating;

    @TableField(exist = false)
    @Schema(description = "评论数量")
    private Integer reviewCount;

} 
