package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourBatchMapper;
import org.example.springboot.mapper.TourOrderMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行程订单支付服务
 * 支持多种支付方式
 */
@Service
public class TourOrderAlipayService {

    private static final Logger logger = LoggerFactory.getLogger(TourOrderAlipayService.class);

    @Autowired
    private TourOrderMapper tourOrderMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AlipayPaymentStrategy alipayPaymentStrategy;

    @Autowired
    private TourOrderNotificationService tourOrderNotificationService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private TourBatchMapper tourBatchMapper;

    @Autowired
    private AdminPermissionService adminPermissionService;

    /**
     * 支付策略注册表
     */
    private final Map<String, PaymentStrategy> strategyRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        registerStrategies();
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
     * 生成支付表单
     *
     * @param orderId 订单ID
     * @param paymentType 支付类型
     * @return 支付表单HTML
     */
    public String generatePayForm(Long orderId, String paymentType) {
        return generatePayForm(orderId, paymentType, null);
    }

    public String generatePayForm(Long orderId, String paymentType, String publicOrigin) {
        if (paymentType == null || paymentType.isEmpty()) {
            paymentType = "alipay";
        }

        logger.info("开始生成支付表单，订单ID: {}, 支付方式: {}", orderId, paymentType);

        // 1. 查询订单
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        // 2. 验证订单状态
        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态异常，无法发起支付");
        }
        assertCanReadOrder(order);
        couponService.revalidateOrderCoupon(order.getId());
        order = tourOrderMapper.selectById(orderId);

        // 3. 验证订单金额
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("订单金额异常");
        }

        // 4. 构建商品描述
        String subject = buildSubject(order);

        // 5. 使用支付策略生成表单
        PaymentStrategy strategy = strategyRegistry.get(paymentType);
        if (strategy == null) {
            throw new ServiceException("不支持的支付方式: " + paymentType);
        }

        if (!strategy.isConfigured()) {
            throw new ServiceException(paymentType + " 未配置或未启用");
        }

        AlipayPaymentStrategy requestAlipayStrategy = null;
        try {
            if (strategy instanceof AlipayPaymentStrategy alipayStrategy && publicOrigin != null && !publicOrigin.isBlank()) {
                requestAlipayStrategy = alipayStrategy;
                requestAlipayStrategy.setRequestCallbackUrls(
                        publicOrigin + "/api/tour-order-pay/return",
                        publicOrigin + "/api/tour-order-pay/notify"
                );
            }
            String formHtml = strategy.generatePayForm(
                    orderId,
                    order.getOrderNo(),
                    order.getTotalAmount().toString(),
                    subject,
                    order.getOrderNo()
            );
            logger.info("支付表单生成成功，订单号: {}, 支付方式: {}", order.getOrderNo(), paymentType);
            return formHtml;
        } catch (Exception e) {
            logger.error("生成支付表单失败，订单号: {}，错误: {}", order.getOrderNo(), e.getMessage());
            throw new ServiceException("生成支付表单失败: " + e.getMessage());
        } finally {
            if (requestAlipayStrategy != null) {
                requestAlipayStrategy.clearRequestCallbackUrls();
            }
        }
    }

    /**
     * 生成支付表单（兼容旧接口，默认支付宝）
     */
    public String generatePayForm(Long orderId) {
        return generatePayForm(orderId, "alipay");
    }

    /**
     * 构建商品描述
     */
    private String buildSubject(TourOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("侠客行国旅-");
        sb.append(order.getTourName());
        if (order.getPackageName() != null && !order.getPackageName().isEmpty()) {
            sb.append("(").append(order.getPackageName()).append(")");
        }
        return sb.toString();
    }

    /**
     * 处理支付宝异步通知
     */
    public String handleNotify(Map<String, String> params) {
        return alipayPaymentStrategy.handleNotify(params);
    }

    /**
     * 校验支付宝同步回跳签名。同步回跳只作为用户体验兜底，最终仍以异步通知为准。
     */
    public boolean verifyAlipayReturn(Map<String, String> params) {
        return alipayPaymentStrategy.verifyNotifySign(params);
    }

    /**
     * 处理支付成功
     */
    @Transactional
    public String processPaySuccess(String outTradeNo, String tradeNo, String notifyAmount) {
        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourOrder::getOrderNo, outTradeNo);
        TourOrder order = tourOrderMapper.selectOne(wrapper);

        if (order == null) {
            logger.error("订单不存在: {}", outTradeNo);
            return "fail";
        }

        if (order.getStatus() == 1) {
            logger.info("订单已支付，跳过处理: {}", outTradeNo);
            return "success";
        }

        if (order.getStatus() != 0) {
            logger.error("订单状态不是待支付，无法支付: {}，状态: {}", outTradeNo, order.getStatus());
            return "fail";
        }

        if (!isNotifyAmountMatched(order, notifyAmount)) {
            logger.error("支付通知金额不匹配: orderNo={}, notifyAmount={}, orderAmount={}",
                    outTradeNo, notifyAmount, order.getTotalAmount());
            return "fail";
        }

        if (!confirmInventory(order)) {
            logger.error("支付确认库存失败: orderNo={}", outTradeNo);
            return "fail";
        }

        order.setStatus(1);
        order.setPaymentMethod("ALIPAY");
        order.setPaymentTime(java.time.LocalDateTime.now());
        order.setUpdateTime(java.time.LocalDateTime.now());

        tourOrderMapper.updateById(order);
        couponService.markUsed(order.getCouponUserId(), order.getId(), order.getOrderNo());

        logger.info("订单支付成功 - 订单号: {}, 交易号: {}", outTradeNo, tradeNo);
        tourOrderNotificationService.notifyPaymentSuccess(order);

        return "success";
    }

    /**
     * 查询订单支付状态
     */
    public TourOrder getPaymentStatus(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        assertCanReadOrder(order);
        return order;
    }

    /**
     * 根据订单号查询订单
     */
    public TourOrder getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourOrder::getOrderNo, orderNo);
        TourOrder order = tourOrderMapper.selectOne(wrapper);
        if (order != null) {
            assertCanReadOrder(order);
        }
        return order;
    }

    /**
     * 模拟支付（用于测试）
     */
    @Transactional
    public void mockPay(Long orderId) {
        logger.info("模拟支付，订单ID: {}", orderId);

        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态异常，无法支付");
        }
        assertCanReadOrder(order);
        if (!confirmInventory(order)) {
            throw new ServiceException("库存确认失败，请稍后重试");
        }

        order.setStatus(1);
        order.setPaymentMethod("MOCK_PAY");
        order.setPaymentTime(java.time.LocalDateTime.now());
        order.setUpdateTime(java.time.LocalDateTime.now());
        tourOrderMapper.updateById(order);
        couponService.markUsed(order.getCouponUserId(), order.getId(), order.getOrderNo());

        logger.info("模拟支付成功，订单ID: {}", orderId);
        tourOrderNotificationService.notifyPaymentSuccess(order);
    }

    /**
     * 获取支付策略
     */
    public PaymentStrategy getStrategy(String paymentType) {
        return strategyRegistry.get(paymentType);
    }

    /**
     * 获取所有支持的支付方式
     */
    public Map<String, PaymentStrategy> getAllStrategies() {
        return strategyRegistry;
    }

    private boolean isNotifyAmountMatched(TourOrder order, String notifyAmount) {
        if (order.getTotalAmount() == null || notifyAmount == null || notifyAmount.isBlank()) {
            return false;
        }
        try {
            return order.getTotalAmount().compareTo(new BigDecimal(notifyAmount)) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean confirmInventory(TourOrder order) {
        TourBatch batch = tourBatchMapper.selectOne(new LambdaQueryWrapper<TourBatch>()
                .eq(TourBatch::getTourId, order.getTourId())
                .eq(TourBatch::getDepartureDate, order.getDepartureDate()));
        if (batch == null) {
            return false;
        }
        int totalPeople = safeCount(order.getAdultCount()) + safeCount(order.getChildCount());
        return totalPeople > 0 && tourBatchMapper.confirmOccupancy(batch.getId(), totalPeople) > 0;
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private void assertCanReadOrder(TourOrder order) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !canManageOrders(currentUser)) {
            throw new ServiceException("无权访问此订单");
        }
    }

    private boolean canManageOrders(User currentUser) {
        return RolePermission.isAdmin(currentUser)
                && (RolePermission.isSuperAdmin(currentUser)
                || adminPermissionService.hasPermission(currentUser, "order:manage"));
    }

}
