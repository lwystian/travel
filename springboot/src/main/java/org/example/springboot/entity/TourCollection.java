package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tour_collection")
@Schema(description = "行程收藏实体类")
public class TourCollection {
    @TableId(type = IdType.AUTO)
    @Schema(description = "收藏ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "行程ID")
    private Long tourId;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;

    @TableField(exist = false)
    @Schema(description = "行程信息")
    private Tour tourInfo;
}
