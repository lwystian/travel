package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_notification")
@Schema(description = "站内消息")
public class SiteNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private String businessType;
    private String businessId;
    private String linkUrl;
    private Integer readStatus;
    private LocalDateTime readTime;
    private String senderType;
    private Long senderId;
    private String senderName;
    private Integer status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String receiverName;
}
