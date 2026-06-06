package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 旅游攻略实体类
 */
@Data
@TableName("travel_guide")
@Schema(description = "旅游攻略实体类")
public class TravelGuide {
    @TableId(type = IdType.AUTO)
    @Schema(description = "攻略ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "作者ID")
    private Long userId;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "浏览量")
    private Integer views;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "发布/最近编辑IP地址")
    private String ipAddress;

    @Schema(description = "网络源端口")
    private Integer port;

    @Schema(description = "浏览器信息")
    private String userAgent;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "设备指纹")
    private String deviceFingerprint;

    @Schema(description = "客户端硬件特征")
    private String clientHardware;

    @Schema(description = "MAC地址")
    private String macAddress;

    @TableField(exist = false)
    @Schema(description = "作者昵称")
    private String userNickname;

    @TableField(exist = false)
    @Schema(description = "作者头像")
    private String userAvatar;

    @TableField(exist = false)
    @Schema(description = "作者角色编码")
    private String userRoleCode;

    @TableField(exist = false)
    @Schema(description = "作者角色名称")
    private String userRoleName;

    @Schema(description = "目的地")
    private String destination;
}
