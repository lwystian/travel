package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tour")
@Schema(description = "行程实体类")
public class Tour {
    @TableId(type = IdType.AUTO)
    @Schema(description = "行程ID")
    private Long id;

    @Schema(description = "行程编号（业务编码）")
    private String code;

    @Schema(description = "行程名称")
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "封面图片URL")
    private String mainImage;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "行程类型，自由填写")
    private String tourType;

    @Schema(description = "出发城市代码")
    private String city;

    @Schema(description = "目的地，可使用地区代码或自由填写")
    private String destination;

    @Schema(description = "行程天数")
    private Integer days;

    @Schema(description = "出发月份")
    private Integer month;

    @Schema(description = "最低价格")
    private BigDecimal minPrice;

    @Schema(description = "评分")
    private BigDecimal starRating;

    @Schema(description = "推荐日期")
    private String recommendDate;

    @Schema(description = "更多日期")
    private String moreDates;

    @Schema(description = "行程特色")
    private String feature;

    @Schema(description = "标签列表（逗号分隔）")
    private String tags;

    @Schema(description = "已报名人数")
    private Integer enrolledCount;

    @Schema(description = "状态: 1-上架, 0-下架")
    private Integer status;

    @Schema(description = "视频URL")
    private String videoUrl;

    @Schema(description = "视频封面URL")
    private String videoPoster;

    @Schema(description = "视频启用状态: 1-启用, 0-禁用")
    private Integer videoEnabled;

    @Schema(description = "多张图片URL（JSON数组存储）")
    private String images;

    @Schema(description = "出团通知")
    private String notice;

    private String detailContent;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
