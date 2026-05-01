package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tour_hotel")
@Schema(description = "行程酒店关联实体类")
public class TourHotel {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "行程ID")
    private Long tourId;

    @Schema(description = "住宿ID（关联accommodation表）")
    private Long accommodationId;

    @Schema(description = "酒店名称")
    private String name;

    @Schema(description = "酒店类型")
    private String type;

    @Schema(description = "每晚价格")
    private BigDecimal pricePerNight;

    @Schema(description = "预订天数")
    private Integer days;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer enabled;

    @Schema(description = "酒店图片URL")
    private String imageUrl;

    @Schema(description = "酒店评分")
    private BigDecimal starLevel;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
