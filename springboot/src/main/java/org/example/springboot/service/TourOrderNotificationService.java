package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TourOrderNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TourOrderNotificationService.class);
    private static final String COMPANY_NAME = "侠客行国际旅行社";
    private static final String COMPANY_SHORT_NAME = "侠客行国旅";
    private static final Duration NOTIFY_DEDUP_TTL = Duration.ofDays(7);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Set<Long> paidOrderNotifyCache = ConcurrentHashMap.newKeySet();

    @Resource
    private UserMapper userMapper;

    @Resource
    private EmailService emailService;

    @Resource
    private AuthConfigService authConfigService;

    @Resource
    private AliyunSmsSenderService aliyunSmsSenderService;

    @Resource
    private SiteNotificationService siteNotificationService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public void notifyPaymentSuccess(TourOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (!markPaidOrderNotified(order.getId())) {
            LOGGER.info("订单支付成功通知已发送过，跳过重复通知。orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            return;
        }

        try {
            User user = userMapper.selectById(order.getUserId());
            sendSiteNotifications(order);
            notifyUser(order, user);
            notifyAdmins(order, user);
            LOGGER.info("订单支付成功通知处理完成。orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        } catch (Exception e) {
            LOGGER.error("订单支付成功通知处理异常。orderId={}, orderNo={}", order.getId(), order.getOrderNo(), e);
        }
    }

    private void sendSiteNotifications(TourOrder order) {
        String userContent = "订单 " + safe(order.getOrderNo())
                + " 已支付成功，我们会为你保留出行名额。请保持预留联系电话 "
                + displayPhoneForUser(order.getContactPhone())
                + " 畅通，稍后将有客服与您进行行程对接，确认出行资料、集合安排和服务细节。";
        siteNotificationService.sendToUser(
                order.getUserId(),
                "订单支付成功",
                userContent,
                "ORDER",
                "TOUR_ORDER",
                String.valueOf(order.getId()),
                "/orders"
        );

        String adminContent = "订单 " + safe(order.getOrderNo())
                + " 已支付成功，请尽快安排客服与用户电话对接。联系人："
                + safe(order.getContactName())
                + "，联系电话：" + safe(order.getContactPhone())
                + "，行程：" + safe(order.getTourName()) + "。";
        siteNotificationService.sendToAdmins(
                "新支付行程订单待跟进",
                adminContent,
                "ORDER",
                "TOUR_ORDER",
                String.valueOf(order.getId()),
                "/back/order"
        );
        LOGGER.info("订单支付成功站内信已创建。orderId={}, orderNo={}, userId={}", order.getId(), order.getOrderNo(), order.getUserId());
    }

    private void notifyUser(TourOrder order, User user) {
        String email = normalizeEmail(user == null ? null : user.getEmail());
        if (StringUtils.hasText(email)) {
            sendEmailQuietly(email, "【" + COMPANY_SHORT_NAME + "】行程订单支付成功通知", buildUserEmail(order, user));
        }

        String phone = normalizePhone(user == null ? null : user.getPhone());
        if (!StringUtils.hasText(phone)) {
            phone = normalizePhone(order.getContactPhone());
        }
        if (StringUtils.hasText(phone)) {
            sendSmsQuietly(phone, true, buildSmsParams(order, user));
        }
    }

    private void notifyAdmins(TourOrder order, User user) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getRoleCode, RolePermission.SUPER_ADMIN, RolePermission.ADMIN)
                .eq(User::getStatus, 1));
        Map<String, Object> params = buildSmsParams(order, user);
        String emailContent = buildAdminEmail(order, user);
        for (User admin : admins) {
            if (!Boolean.TRUE.equals(admin.getOrderNotifyEnabled())) {
                continue;
            }
            String email = normalizeEmail(admin.getEmail());
            if (StringUtils.hasText(email)) {
                sendEmailQuietly(email, "【" + COMPANY_SHORT_NAME + "】新支付行程订单待跟进", emailContent);
            }

            String phone = normalizePhone(admin.getPhone());
            if (StringUtils.hasText(phone)) {
                sendSmsQuietly(phone, false, params);
            }
        }
    }

    private void sendEmailQuietly(String email, String subject, String content) {
        try {
            emailService.sendNotificationEmailAsync(email, subject, content);
        } catch (Exception e) {
            LOGGER.warn("订单通知邮件发送失败。email={}, subject={}, reason={}", maskEmail(email), subject, e.getMessage());
        }
    }

    private void sendSmsQuietly(String phone, boolean userTemplate, Map<String, Object> params) {
        try {
            AliyunSmsConfigDTO config = authConfigService.getSmsConfigForSend();
            String templateCode = userTemplate ? config.getOrderUserTemplateCode() : config.getOrderAdminTemplateCode();
            if (!StringUtils.hasText(templateCode)) {
                LOGGER.warn("订单{}短信模板未配置，跳过发送。phone={}", userTemplate ? "用户" : "管理员", maskPhone(phone));
                return;
            }
            aliyunSmsSenderService.sendTemplate(config, phone, templateCode, params);
        } catch (Exception e) {
            LOGGER.warn("订单通知短信发送失败。phone={}, userTemplate={}, reason={}", maskPhone(phone), userTemplate, e.getMessage());
        }
    }

    private String buildUserEmail(TourOrder order, User user) {
        String displayName = resolveUserName(user, order);
        return """
                %s，您好：

                我们是%s（简称：%s）。您在平台预订的行程订单已支付成功，订单信息如下：

                订单编号：%s
                行程名称：%s
                行程编号：%s
                出发日期：%s
                套餐：%s
                出行人数：成人 %d 人，儿童 %d 人
                联系人：%s
                联系电话：%s
                支付金额：%s 元
                支付方式：%s
                支付时间：%s

                特别提示：下单并支付成功后，%s会安排专门客服尽快通过您预留的联系电话与您电话对接，确认出行资料、集合安排、服务细节和后续注意事项。请保持手机畅通。

                如非您本人操作，请立即联系%s客服核实。

                %s
                """.formatted(
                displayName,
                COMPANY_NAME,
                COMPANY_SHORT_NAME,
                safe(order.getOrderNo()),
                safe(order.getTourName()),
                safe(order.getTourCode()),
                order.getDepartureDate() == null ? "-" : order.getDepartureDate(),
                joinPackageName(order),
                number(order.getAdultCount()),
                number(order.getChildCount()),
                safe(order.getContactName()),
                safe(order.getContactPhone()),
                money(order.getTotalAmount()),
                safe(order.getPaymentMethod()),
                order.getPaymentTime() == null ? "-" : DATE_TIME_FORMATTER.format(order.getPaymentTime()),
                COMPANY_SHORT_NAME,
                COMPANY_SHORT_NAME,
                COMPANY_NAME
        );
    }

    private String buildAdminEmail(TourOrder order, User user) {
        return """
                %s管理员，您好：

                有一笔行程订单已完成支付，请及时安排客服跟进用户电话对接。

                订单编号：%s
                用户账号：%s
                用户昵称：%s
                行程名称：%s
                行程编号：%s
                出发日期：%s
                套餐：%s
                出行人数：成人 %d 人，儿童 %d 人
                联系人：%s
                联系电话：%s
                支付金额：%s 元
                支付方式：%s
                支付时间：%s

                请在后台订单管理中核对订单并完成后续服务分配。

                %s
                """.formatted(
                COMPANY_SHORT_NAME,
                safe(order.getOrderNo()),
                user == null ? "-" : safe(user.getUsername()),
                user == null ? "-" : safe(user.getNickname()),
                safe(order.getTourName()),
                safe(order.getTourCode()),
                order.getDepartureDate() == null ? "-" : order.getDepartureDate(),
                joinPackageName(order),
                number(order.getAdultCount()),
                number(order.getChildCount()),
                safe(order.getContactName()),
                safe(order.getContactPhone()),
                money(order.getTotalAmount()),
                safe(order.getPaymentMethod()),
                order.getPaymentTime() == null ? "-" : DATE_TIME_FORMATTER.format(order.getPaymentTime()),
                COMPANY_NAME
        );
    }

    private Map<String, Object> buildSmsParams(TourOrder order, User user) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("company", COMPANY_SHORT_NAME);
        params.put("orderNo", safe(order.getOrderNo()));
        params.put("tourName", safe(order.getTourName()));
        params.put("departureDate", order.getDepartureDate() == null ? "-" : order.getDepartureDate().toString());
        params.put("amount", money(order.getTotalAmount()));
        params.put("contactName", safe(order.getContactName()));
        params.put("userName", resolveUserName(user, order));
        return params;
    }

    private boolean markPaidOrderNotified(Long orderId) {
        String key = "tour:order:paid-notify:" + orderId;
        if (stringRedisTemplate != null) {
            try {
                Boolean marked = stringRedisTemplate.opsForValue()
                        .setIfAbsent(key, "1", Objects.requireNonNull(NOTIFY_DEDUP_TTL, "notify ttl must not be null"));
                if (Boolean.TRUE.equals(marked)) {
                    return true;
                }
                if (Boolean.FALSE.equals(marked)) {
                    return false;
                }
            } catch (Exception e) {
                LOGGER.warn("订单通知 Redis 去重失败，降级为本地内存去重。orderId={}, reason={}", orderId, e.getMessage());
            }
        }
        return paidOrderNotifyCache.add(orderId);
    }

    private String resolveUserName(User user, TourOrder order) {
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(order.getContactName())) {
            return order.getContactName().trim();
        }
        return "用户";
    }

    private String joinPackageName(TourOrder order) {
        String packageName = safe(order.getPackageName());
        if (StringUtils.hasText(order.getBatchPackageName()) && !"标准".equals(order.getBatchPackageName())) {
            return packageName + " / " + order.getBatchPackageName().trim();
        }
        return packageName;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        String normalized = email.trim().toLowerCase();
        return normalized.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") ? normalized : "";
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        String normalized = phone.trim();
        return normalized.matches("^1[3-9]\\d{9}$") ? normalized : "";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String displayPhoneForUser(String phone) {
        String masked = maskPhone(phone);
        return StringUtils.hasText(masked) ? masked : safe(phone);
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0].length() <= 2 ? parts[0].charAt(0) + "*" : parts[0].substring(0, 2) + "***";
        return name + "@" + parts[1];
    }
}
