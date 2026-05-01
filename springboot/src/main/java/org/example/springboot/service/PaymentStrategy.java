package org.example.springboot.service;

import java.util.Map;

/**
 * 支付策略接口
 * 用于支持多种支付方式的扩展
 */
public interface PaymentStrategy {

    /**
     * 获取支付类型标识
     * @return 支付类型，如 "alipay", "wechat"
     */
    String getPaymentType();

    /**
     * 获取支付名称
     * @return 支付方式名称
     */
    String getPaymentName();

    /**
     * 生成支付表单
     * @param orderId 订单ID
     * @param orderNo 订单号
     * @param amount 支付金额
     * @param subject 订单标题
     * @param description 订单描述
     * @return 支付表单HTML
     */
    String generatePayForm(Long orderId, String orderNo, String amount, String subject, String description);

    /**
     * 验证支付通知签名
     * @param params 通知参数
     * @return 是否验证通过
     */
    boolean verifyNotifySign(Map<String, String> params);

    /**
     * 处理支付成功
     * @param orderNo 订单号
     * @param tradeNo 交易流水号
     * @param amount 交易金额
     * @return 处理结果
     */
    String processPaySuccess(String orderNo, String tradeNo, String amount);

    /**
     * 检查配置是否完整
     * @return 是否已配置
     */
    boolean isConfigured();
}
