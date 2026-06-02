package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.dto.PaymentConfigDTO;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.security.RolePermission;
import org.example.springboot.service.*;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行程订单支付控制器
 * 支持多种支付方式
 */
@Tag(name = "行程订单支付接口")
@RestController
@RequestMapping("/tour-order-pay")
public class TourOrderPayController {

    private static final Logger logger = LoggerFactory.getLogger(TourOrderPayController.class);

    @Resource
    private TourOrderAlipayService tourOrderAlipayService;

    @Resource
    private PaymentConfigController paymentConfigController;

    @Resource
    private Environment environment;

    /**
     * 获取可用的支付方式列表
     */
    @Operation(summary = "获取可用的支付方式列表")
    @GetMapping("/methods")
    public Result<List<PaymentConfigDTO>> getAvailablePaymentMethods() {
        return paymentConfigController.getEnabledPayments();
    }

    /**
     * 生成支付表单
     *
     * @param orderId 订单ID
     * @param paymentType 支付类型（默认支付宝）
     * @return 支付表单HTML
     */
    @Operation(summary = "生成支付表单")
    @GetMapping("/pay/{orderId}")
    public Result<String> generatePayForm(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "alipay") String paymentType,
            HttpServletRequest request) {
        logger.info("请求生成支付表单，订单ID: {}, 支付方式: {}", orderId, paymentType);
        try {
            String formHtml = tourOrderAlipayService.generatePayForm(orderId, paymentType, buildPublicOrigin(request));
            return Result.success(formHtml);
        } catch (Exception e) {
            logger.error("生成支付表单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单支付状态
     */
    @Operation(summary = "获取订单支付状态")
    @GetMapping("/status/{orderId}")
    public Result<TourOrder> getPaymentStatus(@PathVariable Long orderId) {
        try {
            TourOrder order = tourOrderAlipayService.getPaymentStatus(orderId);
            return Result.success(order);
        } catch (Exception e) {
            logger.error("获取订单状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据订单号获取订单信息
     */
    @Operation(summary = "根据订单号获取订单")
    @GetMapping("/order")
    public Result<TourOrder> getOrderByOrderNo(@RequestParam String orderNo) {
        try {
            TourOrder order = tourOrderAlipayService.getOrderByOrderNo(orderNo);
            if (order == null) {
                return Result.error("订单不存在");
            }
            return Result.success(order);
        } catch (Exception e) {
            logger.error("获取订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 模拟支付（仅用于测试）
     */
    @Operation(summary = "模拟支付（仅用于测试）")
    @PostMapping("/mock-pay/{orderId}")
    public Result<?> mockPay(@PathVariable Long orderId) {
        try {
            if (isProdProfile()) {
                throw new ServiceException("生产环境禁止模拟支付");
            }
            if (!RolePermission.isAdmin(JwtTokenUtils.getCurrentUser())) {
                throw new ServiceException("无权限");
            }
            tourOrderAlipayService.mockPay(orderId);
            return Result.success();
        } catch (Exception e) {
            logger.error("模拟支付失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    private boolean isProdProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 支付宝异步通知回调
     */
    @Operation(summary = "支付宝异步通知回调")
    @PostMapping("/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = convertRequestParamsToMap(request);
        logger.info("收到支付宝异步通知: {}", params);
        return tourOrderAlipayService.handleNotify(params);
    }

    /**
     * 同步回调
     */
    @Operation(summary = "同步回调")
    @GetMapping("/return")
    public void alipayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = convertRequestParamsToMap(request);
        logger.info("收到同步回调: {}", params);

        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String amount = params.get("total_amount");
        String status = "failed";

        if (orderNo != null && !orderNo.isEmpty()) {
            try {
                TourOrder order = tourOrderAlipayService.getOrderByOrderNo(orderNo);
                if (order != null) {
                    if (order.getStatus() == 1) {
                        status = "success";
                    } else if (tourOrderAlipayService.verifyAlipayReturn(params)) {
                        logger.info("同步回调验签通过，尝试兜底处理支付: {}", orderNo);
                        String result = tourOrderAlipayService.processPaySuccess(orderNo, tradeNo, amount);
                        if ("success".equals(result)) {
                            status = "success";
                            logger.info("支付处理成功: {}", orderNo);
                        }
                    } else {
                        status = "processing";
                        logger.warn("同步回调验签未通过或订单尚未收到异步通知: {}", orderNo);
                    }
                }
            } catch (Exception e) {
                logger.warn("处理同步回调失败: {}", e.getMessage());
            }
        }

        String redirectUrl = buildReturnUrl(request, orderNo, status);
        logger.info("同步回调处理完成，重定向到: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String buildReturnUrl(HttpServletRequest request, String orderNo, String status) {
        String baseReturnUrl = buildPublicOrigin(request) + "/payment/result";
        return baseReturnUrl
                + "?out_trade_no=" + encode(orderNo)
                + "&status=" + encode(status);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String buildPublicOrigin(HttpServletRequest request) {
        String proto = firstHeader(request, "X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) {
            proto = request.getScheme();
        }
        String host = firstHeader(request, "X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (port > 0 && port != 80 && port != 443) {
                host += ":" + port;
            }
        }
        return proto + "://" + host;
    }

    private String firstHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split(",")[0].trim();
    }

    private Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        Map<String, String> params = new HashMap<>();

        for (String paramName : paramMap.keySet()) {
            String[] values = paramMap.get(paramName);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(paramName, valueStr);
        }

        return params;
    }
}
