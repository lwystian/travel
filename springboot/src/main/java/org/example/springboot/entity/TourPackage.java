package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tour_package")
@Schema(description = "行程套餐实体类")
public class TourPackage {
    @TableId(type = IdType.AUTO)
    @Schema(description = "套餐ID")
    private Long id;

    @Schema(description = "关联行程ID")
    private Long tourId;

    @Schema(description = "套餐名称")
    private String name;

    @Schema(description = "成人价格")
    private BigDecimal adultPrice;

    @Schema(description = "儿童价格")
    private BigDecimal childPrice;

    @Schema(description = "套餐描述")
    private String description;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
