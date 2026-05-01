package org.example.springboot.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TicketOrder;
import org.example.springboot.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlipayService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlipayService.class);

    @Resource
    private AlipayPaymentStrategy alipayPaymentStrategy;

    @Resource
    private TicketOrderService ticketOrderService;

    /**
     * 生成模拟支付宝支付表单（现在只返回简单提示，前端会直接获取订单信息）
     */
    public String createMockAlipayForm(Long orderId) {
        // 获取订单详情
        TicketOrder order = ticketOrderService.getOrderDetail(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        LOGGER.info("生成模拟支付宝支付表单，订单ID: {}, 订单号: {}", orderId, order.getOrderNo());

        // 返回简单的加载提示，实际支付页面由Vue组件处理
        return "<div style='text-align: center; padding: 50px;'>正在加载支付页面...</div>";
    }

    /**
     * 模拟支付宝支付处理
     */
    public void mockAlipayPayment(Long orderId) {
        LOGGER.info("开始处理模拟支付宝支付，订单ID: {}", orderId);

        // 获取订单详情
        TicketOrder order = ticketOrderService.getOrderDetail(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        // 检查订单状态
        if (order.getStatus() == 1) {
            LOGGER.info("订单已支付，无需重复处理");
            return;
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态异常，无法支付");
        }

        try {
            // 模拟支付处理延迟
            Thread.sleep(1000);

            // 更新订单状态为已支付
            ticketOrderService.payOrder(orderId, "ALIPAY");

            LOGGER.info("模拟支付宝支付成功，订单ID: {}, 订单号: {}", orderId, order.getOrderNo());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("支付处理被中断");
        } catch (Exception e) {
            LOGGER.error("模拟支付宝支付失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new ServiceException("支付处理失败: " + e.getMessage());
        }
    }

    /**
     * 生成支付宝支付表单（使用策略模式）
     */
    public String createAlipayForm(Long orderId) {
        // 获取订单详情
        TicketOrder order = ticketOrderService.getOrderDetail(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        try {
            // 使用策略模式生成支付表单
            String subject = "门票预订-" + order.getTicketName();
            return alipayPaymentStrategy.generatePayForm(
                    orderId,
                    order.getOrderNo(),
                    order.getTotalAmount().toString(),
                    subject,
                    order.getOrderNo()
            );
        } catch (ServiceException e) {
            LOGGER.error("生成支付宝支付表单失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.error("生成支付宝支付表单失败: {}", e.getMessage());
            throw new ServiceException("生成支付宝支付表单失败");
        }
    }

    /**
     * 处理支付宝同步回调
     */
    public void handleAlipayReturn(Map<String, String> params) {
        try {
            LOGGER.info("开始处理支付宝同步回调，参数: {}", params);

            // 获取交易信息
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");

            LOGGER.info("支付宝同步回调 - 订单号: {}, 交易号: {}, 交易状态: {}", outTradeNo, tradeNo, tradeStatus);

            // 查询订单
            TicketOrder order = ticketOrderService.getOrderByOrderNo(outTradeNo);
            if (order == null) {
                LOGGER.error("订单不存在: {}", outTradeNo);
                throw new ServiceException("订单不存在");
            }

            LOGGER.info("找到订单: {}, 当前状态: {}", order.getId(), order.getStatus());

            // 订单已支付，则不再处理
            if (order.getStatus() == 1) {
                LOGGER.info("订单已支付，无需重复处理");
                return;
            }

            // 对于同步回调，如果签名验证通过且有交易号，就认为支付成功
            if (tradeNo != null && !tradeNo.isEmpty()) {
                LOGGER.info("开始更新订单状态为已支付，订单ID: {}", order.getId());
                try {
                    ticketOrderService.payOrder(order.getId(), "ALIPAY");
                    LOGGER.info("订单支付状态更新成功，订单号: {}", outTradeNo);
                } catch (Exception e) {
                    LOGGER.error("更新订单支付状态失败，订单号: {}，错误: {}", outTradeNo, e.getMessage(), e);
                    throw e;
                }
            } else {
                LOGGER.error("交易号为空，无法确认支付状态");
                throw new ServiceException("交易号为空");
            }

        } catch (ServiceException e) {
            LOGGER.error("处理支付宝同步回调失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.error("处理支付宝同步回调时发生未知错误: {}", e.getMessage(), e);
            throw new ServiceException("处理支付宝同步回调失败: " + e.getMessage());
        }
    }

    /**
     * 处理支付宝异步通知
     */
    public String handleAlipayNotify(Map<String, String> params) {
        return alipayPaymentStrategy.handleNotify(params);
    }
}
