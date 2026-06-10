package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tour_order")
@Schema(description = "行程订单实体类")
public class TourOrder {
    @TableId(type = IdType.AUTO)
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "行程ID")
    private Long tourId;

    @Schema(description = "行程名称")
    private String tourName;

    @Schema(description = "行程编号")
    private String tourCode;

    @Schema(description = "套餐ID")
    private Long packageId;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "批次套餐ID")
    private Long batchPackageId;

    @Schema(description = "批次套餐名称")
    private String batchPackageName;

    @Schema(description = "附加费用明细JSON")
    private String addonItems;

    @Schema(description = "附加费用摘要")
    private String addonSummary;

    @Schema(description = "出发日期")
    private LocalDate departureDate;

    @Schema(description = "成人数量")
    private Integer adultCount;

    @Schema(description = "儿童数量")
    private Integer childCount;

    @Schema(description = "成人单价")
    private BigDecimal adultUnitPrice;

    @Schema(description = "儿童单价")
    private BigDecimal childUnitPrice;

    @Schema(description = "行程费用（不含酒店）")
    private BigDecimal tourAmount;

    @Schema(description = "酒店ID")
    private Long hotelId;

    @Schema(description = "酒店名称")
    private String hotelName;

    @Schema(description = "酒店住宿天数")
    private Integer hotelDays;

    @Schema(description = "酒店每晚价格")
    private BigDecimal hotelPricePerNight;

    @Schema(description = "酒店总费用")
    private BigDecimal hotelAmount;

    @Schema(description = "用户优惠券ID")
    private Long couponUserId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "应付金额")
    private BigDecimal payableAmount;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人电话")
    private String contactPhone;

    @Schema(description = "订单状态: 0-待支付, 1-已支付, 2-已取消, 3-已退款, 4-已完成")
    private Integer status;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
