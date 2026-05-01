package org.example.springboot.dto;

import lombok.Data;

/**
 * 支付配置DTO（用于前端展示和传输）
 * 敏感信息不会直接返回
 */
@Data
public class PaymentConfigDTO {

    /**
     * 主键ID
     */
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
     * 是否已配置（密钥是否已设置）
     */
    private Boolean configured;
}
