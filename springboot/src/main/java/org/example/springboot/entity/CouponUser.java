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
@TableName("coupon_user")
@Schema(description = "用户优惠券")
public class CouponUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long couponId;
    private Long userId;
    private String couponName;
    private String couponCode;
    private String discountType;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private String scopeType;
    private String scopeIds;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime validStartTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime validEndTime;
    private Integer status;
    private Long orderId;
    private String orderNo;
    private BigDecimal usedAmount;
    private LocalDateTime receiveTime;
    private LocalDateTime lockTime;
    private LocalDateTime useTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
