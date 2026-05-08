package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("home_recommend")
@Schema(description = "首页推荐实体类")
public class HomeRecommend {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "推荐ID")
    private Long id;

    @Schema(description = "推荐类型: featured-精选行程, more-更多推荐")
    private String type;

    @Schema(description = "关联的行程ID")
    private Long tourId;

    @Schema(description = "排序序号（数字越小越靠前）")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
