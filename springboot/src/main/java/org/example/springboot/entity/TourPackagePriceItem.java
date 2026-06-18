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
@TableName("tour_package_price_item")
@Schema(description = "行程套餐价格项")
public class TourPackagePriceItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tourId;

    private Long packageId;

    private String name;

    private BigDecimal adultPrice;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal childPrice;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal originalAdultPrice;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal originalChildPrice;

    private String batchIds;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
