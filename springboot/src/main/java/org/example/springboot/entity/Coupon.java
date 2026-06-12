package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
@Schema(description = "优惠券模板")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer minAge;
    private Integer maxAge;
    private String scopeType;
    private String scopeIds;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer usedQuantity;
    private Integer perUserLimit;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime receiveStartTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime receiveEndTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime validStartTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime validEndTime;
    private Integer stackable;
    private Integer autoReceive;
    private Integer status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
