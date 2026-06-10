package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "行程订单创建请求")
public class TourOrderCreateDTO {

    @NotBlank(message = "行程编号不能为空")
    @Schema(description = "行程编号")
    private String productId;

    @NotNull(message = "套餐ID不能为空")
    @Schema(description = "套餐ID")
    private Long tripPackageId;

    @Schema(description = "批次套餐ID")
    private Long batchPackageId;

    @Schema(description = "附加费用选择列表")
    private List<AddonSelection> addonSelections;

    @NotBlank(message = "出发日期不能为空")
    @Schema(description = "出发日期")
    private String batchDate;

    @NotNull(message = "成人数量不能为空")
    @Min(value = 1, message = "成人数量至少为1")
    @Schema(description = "成人数量")
    private Integer adultCount;

    @Schema(description = "儿童数量")
    private Integer childCount;

    @Schema(description = "酒店ID")
    private Long hotelId;

    @Schema(description = "酒店名称")
    private String hotelName;

    @Schema(description = "酒店住宿天数")
    private Integer hotelDays;

    @Schema(description = "酒店每晚价格")
    private BigDecimal hotelPricePerNight;

    @Schema(description = "用户优惠券ID")
    private Long couponUserId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人电话")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "前端计算的总价（用于校验）")
    private BigDecimal clientTotalPrice;

    @Schema(description = "前端计算的成人单价（用于校验）")
    private BigDecimal clientAdultUnitPrice;

    @Schema(description = "前端计算的儿童单价（用于校验）")
    private BigDecimal clientChildUnitPrice;

    @Data
    @Schema(description = "附加费用选择")
    public static class AddonSelection {
        @NotNull(message = "附加费用ID不能为空")
        @Schema(description = "附加费用ID")
        private Long batchPackageId;

        @Min(value = 1, message = "附加费用数量至少为1")
        @Schema(description = "数量/份数")
        private Integer quantity;
    }
}
