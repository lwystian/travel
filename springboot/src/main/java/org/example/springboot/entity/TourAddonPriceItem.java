package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tour_addon_price_item")
@Schema(description = "附加费用价格项")
public class TourAddonPriceItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tourId;

    private Long addonId;

    private String name;

    private BigDecimal price;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal originalPrice;

    private String batchIds;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
