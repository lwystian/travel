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
@TableName("tour_batch")
@Schema(description = "出发日期实体类")
public class TourBatch {
    @TableId(type = IdType.AUTO)
    @Schema(description = "批次ID")
    private Long id;

    @Schema(description = "关联行程ID")
    private Long tourId;

    @Schema(description = "库存来源：LOCAL/MINIAPP")
    private String sourceType;

    @Schema(description = "外部商品ID")
    private String sourceTourId;

    @Schema(description = "外部班期ID")
    private String sourceScheduleId;

    @Schema(description = "外部接口最近一次返回的可售库存")
    private Integer sourceAvailableStock;

    @Schema(description = "外部库存是否不限量")
    private Boolean sourceUnlimitedStock;

    @Schema(description = "外部库存同步时间")
    private LocalDateTime sourceStockUpdatedAt;

    @Schema(description = "出发日期")
    private LocalDate departureDate;

    @Schema(description = "成人日期附加费")
    private BigDecimal adultDateExtraFee;

    @Schema(description = "儿童日期附加费")
    private BigDecimal childDateExtraFee;

    @Schema(description = "状态: 可报名/已满员/已结束")
    private String status;

    @Schema(description = "余位（可用）")
    private Integer remaining;

    @Schema(description = "锁定库存（已下单未付款占用的名额）")
    private Integer occupied;

    @Schema(description = "最大容量")
    private Integer maxCapacity;

    @Schema(description = "该班期可选行程套餐ID列表(JSON数组，空表示全部)")
    private String packageIds;

    @Schema(description = "该班期可选附加费用ID列表(JSON数组，空表示全部)")
    private String addonIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
