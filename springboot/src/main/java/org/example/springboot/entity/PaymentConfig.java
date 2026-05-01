package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付配置实体类
 */
@Data
@TableName("payment_config")
public class PaymentConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付类型：alipay, wechat, unionpay 等
     */
    private String paymentType;

    /**
     * 支付名称：支付宝、微信支付等
     */
    private String paymentName;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 配置数据（JSON格式，存储敏感信息如密钥）
     */
    private String configData;

    /**
     * 网关地址
     */
    private String gatewayUrl;

    /**
     * 是否沙箱环境
     */
    private Boolean isSandbox;

    /**
     * 图标样式或URL
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
