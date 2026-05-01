package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import org.example.springboot.dto.AlipayConfigDTO;
import org.example.springboot.entity.PaymentConfig;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.PaymentConfigMapper;
import org.example.springboot.mapper.TourOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 支付宝支付策略实现
 */
@Service
public class AlipayPaymentStrategy implements PaymentStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AlipayPaymentStrategy.class);

    private static final String PAYMENT_TYPE = "alipay";
    private static final String GATEWAY_PROD = "https://openapi.alipay.com/gateway.do";
    private static final String GATEWAY_SANDBOX = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    private static final Duration NOTIFY_CACHE_EXPIRE = Duration.ofHours(24);
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final Map<String, Long> notifyIdCache = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "alipay-notify-cache-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    private PaymentConfigMapper paymentConfigMapper;

    @Autowired
    private TourOrderMapper tourOrderMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private AlipayConfigDTO configDTO;

    @PostConstruct
    public void init() {
        cleanupScheduler.scheduleAtFixedRate(() -> cleanupExpiredCache(), 1, 1, TimeUnit.HOURS);
    }

    /**
     * 懒加载配置（从数据库加载配置）
     */
    private void loadConfigFromDatabase() {
        try {
            LambdaQueryWrapper<PaymentConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentConfig::getPaymentType, PAYMENT_TYPE);
            wrapper.eq(PaymentConfig::getEnabled, true);
            PaymentConfig config = paymentConfigMapper.selectOne(wrapper);

            if (config != null && config.getConfigData() != null) {
                this.configDTO = JSON.parseObject(config.getConfigData(), AlipayConfigDTO.class);
                this.configDTO.setGatewayUrl(config.getIsSandbox() ? GATEWAY_SANDBOX : GATEWAY_PROD);
                this.configDTO.setIsSandbox(config.getIsSandbox());
                logger.info("从数据库加载支付宝配置成功");
            } else {
                logger.warn("未找到启用的支付宝配置");
            }
        } catch (Exception e) {
            logger.error("加载支付宝配置失败", e);
        }
    }

    /**
     * 确保配置已加载
     */
    private void ensureConfigLoaded() {
        if (configDTO == null) {
            loadConfigFromDatabase();
        }
    }

    /**
     * 获取实际网关地址
     */
    private String getActualGateway() {
        if (configDTO != null && Boolean.TRUE.equals(configDTO.getIsSandbox())) {
            return GATEWAY_SANDBOX;
        }
        return GATEWAY_PROD;
    }

    @Override
    public String getPaymentType() {
        return PAYMENT_TYPE;
    }

    @Override
    public String getPaymentName() {
        return "支付宝";
    }

    @Override
    public String generatePayForm(Long orderId, String orderNo, String amount, String subject, String description) {
        ensureConfigLoaded();
        if (!isConfigured()) {
            throw new ServiceException("支付宝未配置或未启用");
        }

        try {
            AlipayClient alipayClient = createAlipayClient();
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setReturnUrl(configDTO.getReturnUrl());
            request.setNotifyUrl(configDTO.getNotifyUrl());

            String timeoutExpress = configDTO.getTimeoutExpress();
            if (timeoutExpress == null || timeoutExpress.isEmpty()) {
                timeoutExpress = "2h";
            }
            
            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"body\":\"%s\",\"timeout_express\":\"%s\"}",
                    orderNo, amount, subject, description != null ? description : subject, timeoutExpress
            );
            request.setBizContent(bizContent);

            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            logger.error("生成支付宝支付表单失败", e);
            throw new ServiceException("生成支付表单失败: " + e.getMessage());
        }
    }

    private AlipayClient createAlipayClient() {
        return new DefaultAlipayClient(
                getActualGateway(),
                configDTO.getAppId(),
                configDTO.getPrivateKey(),
                "json",
                "UTF-8",
                configDTO.getAlipayPublicKey(),
                "RSA2"
        );
    }

    @Override
    public boolean verifyNotifySign(Map<String, String> params) {
        if (!isConfigured()) {
            return false;
        }

        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    configDTO.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
            logger.error("验签失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public String processPaySuccess(String orderNo, String tradeNo, String amount) {
        TourOrder order = tourOrderMapper.selectOne(
                new LambdaQueryWrapper<TourOrder>().eq(TourOrder::getOrderNo, orderNo)
        );

        if (order == null) {
            logger.warn("订单不存在: {}", orderNo);
            return "failure";
        }

        if (order.getStatus() == 1) {
            logger.info("订单已支付，忽略重复通知: {}", orderNo);
            return "success";
        }

        order.setStatus(1);
        order.setPaymentTime(LocalDateTime.now());
        order.setPaymentMethod("alipay");

        tourOrderMapper.updateById(order);
        logger.info("订单支付成功: {}, 金额: {}", orderNo, amount);
        return "success";
    }

    @Override
    public boolean isConfigured() {
        ensureConfigLoaded();
        return configDTO != null
                && configDTO.getAppId() != null && !configDTO.getAppId().isEmpty()
                && configDTO.getPrivateKey() != null && !configDTO.getPrivateKey().isEmpty()
                && configDTO.getAlipayPublicKey() != null && !configDTO.getAlipayPublicKey().isEmpty();
    }

    /**
     * 处理异步通知
     */
    public String handleNotify(Map<String, String> params) {
        logger.info("收到支付宝异步通知: {}", params);

        if (!verifyNotifySign(params)) {
            logger.error("支付宝异步通知验签失败");
            return "failure";
        }

        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String amount = params.get("total_amount");

            if (!isNotifyProcessed(params.get("notify_id"))) {
                String result = processPaySuccess(orderNo, tradeNo, amount);
                if ("success".equals(result)) {
                    markNotifyProcessed(params.get("notify_id"));
                }
                return result;
            }
        }

        return "success";
    }

    private boolean isNotifyProcessed(String notifyId) {
        if (notifyId == null || notifyId.isEmpty()) {
            return false;
        }

        if (stringRedisTemplate != null) {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey("alipay:notify:" + notifyId));
        }
        return notifyIdCache.containsKey(notifyId);
    }

    private void markNotifyProcessed(String notifyId) {
        if (notifyId == null || notifyId.isEmpty()) {
            return;
        }

        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(
                    "alipay:notify:" + notifyId,
                    "1",
                    NOTIFY_CACHE_EXPIRE
            );
        } else {
            notifyIdCache.put(notifyId, System.currentTimeMillis() + NOTIFY_CACHE_EXPIRE.toMillis());
        }
    }

    private void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        notifyIdCache.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    /**
     * 模拟支付（仅用于测试）
     */
    @Transactional
    public void mockPay(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() == 1) {
            throw new ServiceException("订单已支付");
        }

        order.setStatus(1);
        order.setPaymentTime(LocalDateTime.now());
        order.setPaymentMethod("alipay");
        tourOrderMapper.updateById(order);
        logger.info("模拟支付成功，订单ID: {}", orderId);
    }

    /**
     * 更新配置（由PaymentConfigService调用）
     */
    public void updateConfig(PaymentConfig config) {
        if (PAYMENT_TYPE.equals(config.getPaymentType())) {
            if (config.getConfigData() != null) {
                this.configDTO = JSON.parseObject(config.getConfigData(), AlipayConfigDTO.class);
                this.configDTO.setGatewayUrl(config.getIsSandbox() ? GATEWAY_SANDBOX : GATEWAY_PROD);
                this.configDTO.setIsSandbox(config.getIsSandbox());
                logger.info("更新支付宝配置成功");
            }
        }
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        this.configDTO = null;
        loadConfigFromDatabase();
    }
}
