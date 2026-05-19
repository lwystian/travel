package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("accommodation_review")
@Schema(description = "住宿评价实体类")
public class AccommodationReview {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "评价ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "住宿ID")
    private Integer accommodationId;
    
    @Schema(description = "评价内容")
    private String content;
    
    @Schema(description = "评分")
    private BigDecimal rating;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "审核状态: 0-待审核, 1-已通过, 2-已拒绝")
    private Integer reviewStatus;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人用户名")
    private String reviewerName;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "审核意见")
    private String reviewComment;
    
    @TableField(exist = false)
    @Schema(description = "用户昵称")
    private String nickname;
    
    @TableField(exist = false)
    @Schema(description = "用户头像")
    private String avatar;

    @TableField(exist = false)
    @Schema(description = "住宿名称")
    private String accommodationName;
} 
