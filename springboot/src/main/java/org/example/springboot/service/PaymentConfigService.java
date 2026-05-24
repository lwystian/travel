package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import org.example.springboot.dto.AlipayConfigDTO;
import org.example.springboot.dto.PaymentConfigDTO;
import org.example.springboot.entity.PaymentConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.PaymentConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付配置服务
 */
@Service
public class PaymentConfigService extends ServiceImpl<PaymentConfigMapper, PaymentConfig> {

    private static final Logger logger = LoggerFactory.getLogger(PaymentConfigService.class);

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 支付策略注册表
     */
    private final Map<String, PaymentStrategy> strategyRegistry = new ConcurrentHashMap<>();

    /**
     * 已启用的支付方式缓存
     */
    private volatile List<PaymentConfigDTO> enabledPaymentsCache = null;

    @PostConstruct
    public void init() {
        registerStrategies();
        initializeDefaultConfigs();
    }

    /**
     * 注册所有支付策略
     */
    private void registerStrategies() {
        Map<String, PaymentStrategy> strategies = applicationContext.getBeansOfType(PaymentStrategy.class);
        strategies.forEach((name, strategy) -> {
            strategyRegistry.put(strategy.getPaymentType(), strategy);
            logger.info("注册支付策略: {} - {}", strategy.getPaymentType(), strategy.getPaymentName());
        });
    }

    /**
     * 初始化默认配置
     */
    private void initializeDefaultConfigs() {
        if (count() == 0) {
            logger.info("初始化默认支付配置");

            PaymentConfig alipay = new PaymentConfig();
            alipay.setPaymentType("alipay");
            alipay.setPaymentName("支付宝");
            alipay.setEnabled(false);
            alipay.setIsSandbox(true);
            alipay.setGatewayUrl("https://openapi.alipay.com/gateway.do");
            alipay.setIcon("alipay");
            alipay.setDescription("支付宝网页支付");
            alipay.setSortOrder(1);
            alipay.setCreatedAt(LocalDateTime.now());
            alipay.setUpdatedAt(LocalDateTime.now());

            AlipayConfigDTO defaultAlipay = new AlipayConfigDTO();
            defaultAlipay.setAppId("");
            defaultAlipay.setPrivateKey("");
            defaultAlipay.setAlipayPublicKey("");
            alipay.setConfigData(JSON.toJSONString(defaultAlipay));

            save(alipay);
        }
    }

    /**
     * 获取已启用的支付方式列表
     */
    public List<PaymentConfigDTO> getEnabledPayments() {
        if (enabledPaymentsCache != null) {
            return enabledPaymentsCache;
        }

        List<PaymentConfig> configs = list(
                new LambdaQueryWrapper<PaymentConfig>()
                        .eq(PaymentConfig::getEnabled, true)
                        .orderByAsc(PaymentConfig::getSortOrder)
        );

        List<PaymentConfigDTO> result = new ArrayList<>();
        for (PaymentConfig config : configs) {
            PaymentConfigDTO dto = convertToDTO(config);
            result.add(dto);
        }

        enabledPaymentsCache = result;
        return result;
    }

    /**
     * 获取所有支付配置
     */
    public List<PaymentConfigDTO> getAllPayments() {
        List<PaymentConfig> configs = list(
                new LambdaQueryWrapper<PaymentConfig>()
                        .orderByAsc(PaymentConfig::getSortOrder)
        );

        List<PaymentConfigDTO> result = new ArrayList<>();
        for (PaymentConfig config : configs) {
            result.add(convertToDTO(config));
        }
        return result;
    }

    /**
     * 转换为DTO
     */
    private PaymentConfigDTO convertToDTO(PaymentConfig config) {
        PaymentConfigDTO dto = new PaymentConfigDTO();
        dto.setId(config.getId());
        dto.setPaymentType(config.getPaymentType());
        dto.setPaymentName(config.getPaymentName());
        dto.setEnabled(config.getEnabled());
        dto.setGatewayUrl(config.getGatewayUrl());
        dto.setIsSandbox(config.getIsSandbox());
        dto.setIcon(config.getIcon());
        dto.setDescription(config.getDescription());
        dto.setSortOrder(config.getSortOrder());

        // 检查是否已配置
        boolean configured = false;
        if ("alipay".equals(config.getPaymentType()) && config.getConfigData() != null) {
            try {
                AlipayConfigDTO alipayConfig = JSON.parseObject(config.getConfigData(), AlipayConfigDTO.class);
                configured = alipayConfig.getAppId() != null && !alipayConfig.getAppId().isEmpty()
                        && alipayConfig.getPrivateKey() != null && !alipayConfig.getPrivateKey().isEmpty()
                        && alipayConfig.getAlipayPublicKey() != null && !alipayConfig.getAlipayPublicKey().isEmpty();
            } catch (Exception e) {
                logger.error("解析支付配置失败", e);
            }
        }
        dto.setConfigured(configured);

        return dto;
    }

    /**
     * 根据ID获取配置
     */
    public PaymentConfig getById(Long id) {
        PaymentConfig config = super.getById(id);
        if (config == null) {
            throw new ServiceException("配置不存在");
        }
        return config;
    }

    /**
     * 根据类型获取配置
     */
    public PaymentConfig getByType(String paymentType) {
        return getOne(new LambdaQueryWrapper<PaymentConfig>()
                .eq(PaymentConfig::getPaymentType, paymentType));
    }

    /**
     * 获取支付配置详情（通用方法）
     */
    public AlipayConfigDTO getPaymentConfigDetail(String paymentType) {
        PaymentConfig config = getByType(paymentType);
        if (config == null || config.getConfigData() == null) {
            return new AlipayConfigDTO();
        }
        return JSON.parseObject(config.getConfigData(), AlipayConfigDTO.class);
    }

    /**
     * 获取支付宝配置详情（兼容旧接口）
     */
    public AlipayConfigDTO getAlipayConfigDetail() {
        return getPaymentConfigDetail("alipay");
    }

    /**
     * 保存支付配置
     */
    @Transactional
    public PaymentConfig savePayment(PaymentConfig config) {
        // 新增时检查支付类型是否已存在
        if (config.getId() == null) {
            PaymentConfig existing = getByType(config.getPaymentType());
            if (existing != null) {
                throw new ServiceException("当前支付方式已存在，无法重复添加");
            }
            config.setCreatedAt(LocalDateTime.now());
        }
        config.setUpdatedAt(LocalDateTime.now());

        saveOrUpdate(config);
        clearCache();
        notifyStrategyUpdate(config);

        return config;
    }

    /**
     * 更新支付配置
     */
    @Transactional
    public void updatePayment(Long id, PaymentConfig config) {
        PaymentConfig existing = getById(id);
        config.setId(id);
        config.setCreatedAt(existing.getCreatedAt());
        config.setUpdatedAt(LocalDateTime.now());

        saveOrUpdate(config);
        clearCache();
        notifyStrategyUpdate(config);
    }

    /**
     * 保存支付宝配置详情
     */
    @Transactional
    public void saveAlipayConfigDetail(AlipayConfigDTO alipayConfig) {
        PaymentConfig config = getByType(alipayConfig.getPaymentType());
        if (config == null) {
            config = new PaymentConfig();
            config.setPaymentType(alipayConfig.getPaymentType());
            config.setPaymentName("alipay".equals(alipayConfig.getPaymentType()) ? "支付宝" : "微信支付");
            config.setCreatedAt(LocalDateTime.now());
        }

        config.setConfigData(JSON.toJSONString(alipayConfig));

        // 设置网关地址，优先使用用户自定义的，否则使用默认值
        boolean isSandbox = alipayConfig.getIsSandbox() != null && alipayConfig.getIsSandbox();
        String gatewayUrl = alipayConfig.getGatewayUrl();
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            gatewayUrl = isSandbox ? "https://openapi-sandbox.dl.alipaydev.com/gateway.do" : "https://openapi.alipay.com/gateway.do";
        }
        config.setGatewayUrl(gatewayUrl);
        config.setIsSandbox(isSandbox);
        config.setUpdatedAt(LocalDateTime.now());

        saveOrUpdate(config);
        clearCache();
        notifyStrategyUpdate(config);
    }

    /**
     * 切换启用状态
     */
    @Transactional
    public void toggleEnabled(Long id, boolean enabled) {
        PaymentConfig config = getById(id);
        config.setEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());
        updateById(config);
        clearCache();
        notifyStrategyUpdate(config);
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deletePayment(Long id) {
        removeById(id);
        clearCache();
    }

    /**
     * 清除缓存
     */
    private void clearCache() {
        enabledPaymentsCache = null;
    }

    /**
     * 通知策略更新
     */
    private void notifyStrategyUpdate(PaymentConfig config) {
        PaymentStrategy strategy = strategyRegistry.get(config.getPaymentType());
        if (strategy instanceof AlipayPaymentStrategy) {
            ((AlipayPaymentStrategy) strategy).updateConfig(config);
        }
    }

    /**
     * 获取支付策略
     */
    public PaymentStrategy getStrategy(String paymentType) {
        return strategyRegistry.get(paymentType);
    }
}
