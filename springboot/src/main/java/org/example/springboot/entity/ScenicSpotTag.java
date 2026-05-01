package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("scenic_spot_tag")
@Schema(description = "景点标签关联实体类")
public class ScenicSpotTag {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "景点ID")
    private Long scenicSpotId;
    
    @Schema(description = "标签ID")
    private Long tagId;
}
