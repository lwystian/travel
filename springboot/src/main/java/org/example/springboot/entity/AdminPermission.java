package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_permission")
@Schema(description = "管理员后台权限")
public class AdminPermission {
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "管理员用户ID")
    private Long userId;

    @Schema(description = "权限编码")
    private String permissionCode;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
