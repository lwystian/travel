package org.example.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 支付宝配置详情DTO（用于编辑保存）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlipayConfigDTO {

    private String paymentType;

    private String paymentName;

    private String description;

    /**
     * 支付宝应用ID
     */
    private String appId;

    /**
     * 商户私钥（PKCS8格式）
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 回调地址（异步通知）
     */
    private String notifyUrl;

    /**
     * 同步跳转地址
     */
    private String returnUrl;

    /**
     * 网关地址
     */
    private String gatewayUrl;

    /**
     * 是否沙箱环境
     */
    private Boolean isSandbox;

    /**
     * 支付超时时间（如 "2h" 表示2小时）
     */
    private String timeoutExpress;
}
