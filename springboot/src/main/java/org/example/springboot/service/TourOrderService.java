package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.dto.TourOrderCreateDTO;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.example.springboot.security.RolePermission;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class TourOrderService {

    private static final Logger logger = LoggerFactory.getLogger(TourOrderService.class);
    private static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.01");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private TourOrderMapper tourOrderMapper;

    @Resource
    private TourMapper tourMapper;

    @Resource
    private TourPackageMapper tourPackageMapper;

    @Resource
    private BatchPackageMapper batchPackageMapper;

    @Resource
    private TourBatchMapper tourBatchMapper;
    @Resource
    private SiteNotificationService siteNotificationService;

    @Resource
    private TourOrderNotificationService tourOrderNotificationService;

    @Resource
    private CouponService couponService;

    @Resource
    private AdminPermissionService adminPermissionService;

    /**
     * 创建行程订单
     */
    @Transactional
    public TourOrder createOrder(TourOrderCreateDTO dto) {
        // 1. 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }

        // 2. 根据行程编号查询行程
        LambdaQueryWrapper<Tour> tourWrapper = new LambdaQueryWrapper<>();
        tourWrapper.eq(Tour::getCode, dto.getProductId());
        Tour tour = tourMapper.selectOne(tourWrapper);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }
        if (tour.getStatus() != 1) {
            throw new ServiceException("该行程暂不可预订");
        }

        // 3. 检查用户是否已有该行程的未支付订单（防止恶意刷单）
        LambdaQueryWrapper<TourOrder> existingOrderWrapper = new LambdaQueryWrapper<>();
        existingOrderWrapper.eq(TourOrder::getUserId, currentUser.getId())
                          .eq(TourOrder::getTourId, tour.getId())
                          .eq(TourOrder::getStatus, 0); // 待支付状态
        List<TourOrder> existingOrders = tourOrderMapper.selectList(existingOrderWrapper);
        if (existingOrders != null && !existingOrders.isEmpty()) {
            if (existingOrders.size() == 1) {
                throw new ServiceException("您已有一笔该行程的待支付订单，请先完成支付或取消后再重新预订");
            } else {
                throw new ServiceException("您已有" + existingOrders.size() + "笔该行程的待支付订单，请先完成支付或取消后再重新预订");
            }
        }

        // 4. 验证套餐
        TourPackage tourPackage = tourPackageMapper.selectById(dto.getTripPackageId());
        if (tourPackage == null) {
            throw new ServiceException("套餐不存在");
        }
        if (!tourPackage.getTourId().equals(tour.getId())) {
            throw new ServiceException("套餐与行程不匹配");
        }
        if (tourPackage.getStatus() != 1) {
            throw new ServiceException("该套餐暂不可预订");
        }

        // 5. 验证出发日期批次
        LocalDate departureDate;
        TourBatch tourBatch;
        try {
            departureDate = LocalDate.parse(dto.getBatchDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new ServiceException("日期格式错误");
        }

        LambdaQueryWrapper<TourBatch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(TourBatch::getTourId, tour.getId())
                   .eq(TourBatch::getDepartureDate, departureDate);
        tourBatch = tourBatchMapper.selectOne(batchWrapper);
        if (tourBatch == null) {
            throw new ServiceException("该出发日期不存在");
        }
        if (!"可报名".equals(tourBatch.getStatus())) {
            throw new ServiceException("该批次" + tourBatch.getStatus() + "，不可预订");
        }
        Set<Long> availablePackageIds = parseIdSet(tourBatch.getPackageIds());
        if (!availablePackageIds.isEmpty() && !availablePackageIds.contains(tourPackage.getId())) {
            throw new ServiceException("该出发日期不支持所选行程套餐");
        }

        // 6. 计算总人数
        int totalPeople = dto.getAdultCount() + (dto.getChildCount() != null ? dto.getChildCount() : 0);

        // 7. 原子锁定库存（解决并发问题）
        // 使用 UPDATE ... WHERE (remaining - occupied) >= totalPeople 保证原子性
        int lockResult = tourBatchMapper.lockOccupancy(tourBatch.getId(), totalPeople);
        if (lockResult == 0) {
            // 锁定失败，余位不足
            // 重新查询当前余位，给出准确的错误提示
            tourBatch = tourBatchMapper.selectById(tourBatch.getId());
            int currentAvailable = tourBatch.getRemaining() - (tourBatch.getOccupied() != null ? tourBatch.getOccupied() : 0);
            throw new ServiceException("余位不足，当前剩余" + currentAvailable + "个名额");
        }

        Long lockedCouponUserId = null;
        try {
            // 8. 获取附加费用及份数
            AddonPriceResult addonPriceResult = calculateAddonPrice(tour, tourBatch, dto, totalPeople);

            // 9. 后端精确计算价格
            BigDecimal adultUnitPrice = calculateAdultUnitPrice(tourPackage, tourBatch);
            BigDecimal childUnitPrice = calculateChildUnitPrice(tourPackage, tourBatch);

            // 10. 验证前端传来的价格（允许0.01元误差）
            if (dto.getClientAdultUnitPrice() != null) {
                BigDecimal diff = adultUnitPrice.subtract(dto.getClientAdultUnitPrice()).abs();
                if (diff.compareTo(PRICE_TOLERANCE) > 0) {
                    logger.warn("成人单价校验失败：前端={}, 后端={}, 差异={}",
                        dto.getClientAdultUnitPrice(), adultUnitPrice, diff);
                    throw new ServiceException("价格已变更，请刷新页面后重试");
                }
            }

            if (dto.getClientChildUnitPrice() != null && dto.getClientChildUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = childUnitPrice.subtract(dto.getClientChildUnitPrice()).abs();
                if (diff.compareTo(PRICE_TOLERANCE) > 0) {
                    logger.warn("儿童单价校验失败：前端={}, 后端={}, 差异={}",
                        dto.getClientChildUnitPrice(), childUnitPrice, diff);
                    throw new ServiceException("价格已变更，请刷新页面后重试");
                }
            }

            // 11. 计算行程费用
            BigDecimal tourAmount = adultUnitPrice.multiply(new BigDecimal(dto.getAdultCount()));
            if (dto.getChildCount() != null && dto.getChildCount() > 0) {
                tourAmount = tourAmount.add(childUnitPrice.multiply(new BigDecimal(dto.getChildCount())));
            }
            tourAmount = tourAmount.add(addonPriceResult.totalAmount());

            // 12. 计算酒店费用（如有）
            BigDecimal hotelAmount = BigDecimal.ZERO;
            if (dto.getHotelId() != null && dto.getHotelDays() != null && dto.getHotelDays() > 0) {
                if (dto.getHotelPricePerNight() == null || dto.getHotelPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("酒店价格信息错误");
                }
                hotelAmount = dto.getHotelPricePerNight().multiply(new BigDecimal(dto.getHotelDays()));
            }

            // 13. 计算订单总金额
            BigDecimal totalAmount = tourAmount.add(hotelAmount);
            CouponService.CouponUseResult couponResult = couponService.lockForOrder(dto.getCouponUserId(), tour, tourPackage, totalAmount, null, null);
            lockedCouponUserId = couponResult.couponUserId();
            BigDecimal discountAmount = couponResult.discountAmount();
            BigDecimal payableAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO);

            // 14. 验证前端传来的总价（允许0.01元误差）
            if (dto.getClientTotalPrice() != null) {
                BigDecimal diff = payableAmount.subtract(dto.getClientTotalPrice()).abs();
                if (diff.compareTo(PRICE_TOLERANCE) > 0) {
                    logger.warn("总价校验失败：前端={}, 后端={}, 差异={}",
                        dto.getClientTotalPrice(), payableAmount, diff);
                    throw new ServiceException("价格已变更，请刷新页面后重试");
                }
            }

            // 15. 创建订单
            TourOrder order = new TourOrder();
            order.setOrderNo(generateOrderNo());
            order.setUserId(currentUser.getId());
            order.setTourId(tour.getId());
            order.setTourName(tour.getTitle());
            order.setTourCode(tour.getCode());
            order.setPackageId(tourPackage.getId());
            order.setPackageName(tourPackage.getName());
            order.setBatchPackageId(addonPriceResult.primaryAddonId());
            order.setBatchPackageName(addonPriceResult.summary());
            order.setAddonItems(addonPriceResult.itemsJson());
            order.setAddonSummary(addonPriceResult.summary());
            order.setDepartureDate(departureDate);
            order.setAdultCount(dto.getAdultCount());
            order.setChildCount(dto.getChildCount() != null ? dto.getChildCount() : 0);
            order.setAdultUnitPrice(adultUnitPrice);
            order.setChildUnitPrice(childUnitPrice);
            order.setTourAmount(tourAmount);
            order.setHotelId(dto.getHotelId());
            order.setHotelName(dto.getHotelName());
            order.setHotelDays(dto.getHotelDays());
            order.setHotelPricePerNight(dto.getHotelPricePerNight());
            order.setHotelAmount(hotelAmount);
            order.setCouponUserId(couponResult.couponUserId());
            order.setCouponName(couponResult.couponName());
            order.setDiscountAmount(discountAmount);
            order.setPayableAmount(payableAmount);
            order.setTotalAmount(payableAmount);
            order.setContactName(dto.getContactName());
            order.setContactPhone(dto.getContactPhone());
            order.setRemark(dto.getRemark());
            order.setStatus(0); // 待支付
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            // 16. 保存订单
            tourOrderMapper.insert(order);
            if (couponResult.couponUserId() != null) {
                couponService.bindLockedOrder(couponResult.couponUserId(), order.getId(), order.getOrderNo());
            }

            logger.info("行程订单创建成功：订单号={}, 用户={}, 行程={}, 总价={}, 锁定人数={}",
                order.getOrderNo(), currentUser.getUsername(), tour.getTitle(), totalAmount, totalPeople);
            siteNotificationService.sendToUser(
                    currentUser.getId(),
                    "订单已创建，请及时付款",
                    "你已成功提交订单 " + order.getOrderNo() + "，请在订单有效期内完成支付。",
                    "ORDER",
                    "TOUR_ORDER",
                    String.valueOf(order.getId()),
                    "/orders"
            );
            siteNotificationService.sendToAdmins(
                    "新的待付款订单",
                    currentUser.getUsername()
                            + " 提交了行程订单 " + order.getOrderNo()
                            + "。联系人：" + order.getContactName()
                            + "，联系电话：" + order.getContactPhone()
                            + "，行程：" + order.getTourName()
                            + "，出行人数：成人 " + order.getAdultCount() + " 人"
                            + (order.getChildCount() != null && order.getChildCount() > 0 ? "，儿童 " + order.getChildCount() + " 人" : "")
                            + "，订单金额：" + order.getTotalAmount() + " 元。请关注支付状态并做好后续对接准备。",
                    "ORDER",
                    "TOUR_ORDER",
                    String.valueOf(order.getId()),
                    "/back/order"
            );

            return copyMaskedOrderForUser(order);

        } catch (ServiceException e) {
            // 业务异常：释放已锁定的库存
            releaseBatchOccupancy(tourBatch.getId(), totalPeople);
            couponService.releaseLocked(lockedCouponUserId);
            throw e;
        } catch (Exception e) {
            // 其他异常：释放已锁定的库存
            releaseBatchOccupancy(tourBatch.getId(), totalPeople);
            couponService.releaseLocked(lockedCouponUserId);
            throw new ServiceException("订单创建失败：" + e.getMessage());
        }
    }

    private AddonPriceResult calculateAddonPrice(Tour tour, TourBatch tourBatch, TourOrderCreateDTO dto, int totalPeople) {
        List<TourOrderCreateDTO.AddonSelection> selections = new ArrayList<>();
        if (dto.getAddonSelections() != null && !dto.getAddonSelections().isEmpty()) {
            selections.addAll(dto.getAddonSelections());
        } else if (dto.getBatchPackageId() != null) {
            TourOrderCreateDTO.AddonSelection legacySelection = new TourOrderCreateDTO.AddonSelection();
            legacySelection.setBatchPackageId(dto.getBatchPackageId());
            legacySelection.setQuantity(Math.max(1, totalPeople));
            selections.add(legacySelection);
        }
        if (selections.isEmpty()) {
            return new AddonPriceResult(BigDecimal.ZERO, null, "无", null);
        }

        Set<Long> availableAddonIds = parseIdSet(tourBatch.getAddonIds());
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Map<String, Object>> itemList = new ArrayList<>();
        List<String> summaryParts = new ArrayList<>();
        Long primaryAddonId = null;

        for (TourOrderCreateDTO.AddonSelection selection : selections) {
            if (selection == null || selection.getBatchPackageId() == null) {
                continue;
            }
            int quantity = selection.getQuantity() != null ? selection.getQuantity() : 0;
            if (quantity <= 0) {
                continue;
            }
            if (!availableAddonIds.isEmpty() && !availableAddonIds.contains(selection.getBatchPackageId())) {
                throw new ServiceException("该出发日期不支持所选附加费用");
            }
            BatchPackage addon = batchPackageMapper.selectById(selection.getBatchPackageId());
            if (addon == null || !addon.getTourId().equals(tour.getId()) || !Integer.valueOf(1).equals(addon.getStatus())) {
                throw new ServiceException("附加费用不存在或已停用");
            }
            BigDecimal unitPrice = addon.getExtraFeePerPerson() != null ? addon.getExtraFeePerPerson() : BigDecimal.ZERO;
            BigDecimal itemAmount = unitPrice.multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemAmount);
            if (primaryAddonId == null) {
                primaryAddonId = addon.getId();
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", addon.getId());
            item.put("name", addon.getName());
            item.put("unitPrice", unitPrice);
            item.put("quantity", quantity);
            item.put("amount", itemAmount);
            itemList.add(item);
            summaryParts.add(addon.getName() + " x" + quantity);
        }

        if (itemList.isEmpty()) {
            return new AddonPriceResult(BigDecimal.ZERO, null, "无", null);
        }
        String itemsJson = null;
        try {
            itemsJson = OBJECT_MAPPER.writeValueAsString(itemList);
        } catch (Exception e) {
            logger.warn("Serialize addon items failed", e);
        }
        return new AddonPriceResult(totalAmount, primaryAddonId, String.join("，", summaryParts), itemsJson);
    }

    /**
     * 释放锁定库存（异常时回滚）
     */
    private void releaseBatchOccupancy(Long batchId, int totalPeople) {
        try {
            LambdaQueryWrapper<TourBatch> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TourBatch::getId, batchId);
            TourBatch batch = tourBatchMapper.selectOne(wrapper);
            if (batch != null) {
                int currentOccupied = batch.getOccupied() != null ? batch.getOccupied() : 0;
                batch.setOccupied(Math.max(0, currentOccupied - totalPeople));
                tourBatchMapper.updateById(batch);
                logger.info("异常回滚释放锁定库存：批次={}, 人数={}, 当前锁定={}", batchId, totalPeople, batch.getOccupied());
            }
        } catch (Exception e) {
            logger.error("释放锁定库存失败：批次={}, 错误={}", batchId, e.getMessage());
        }
    }

    /**
     * 计算成人单价：套餐价 + 日期附加费 + 批次附加费
     */
    private BigDecimal calculateAdultUnitPrice(TourPackage tourPackage, TourBatch tourBatch) {
        BigDecimal price = tourPackage.getAdultPrice() != null ? tourPackage.getAdultPrice() : BigDecimal.ZERO;
        if (tourBatch.getAdultDateExtraFee() != null) {
            price = price.add(tourBatch.getAdultDateExtraFee());
        }
        return price;
    }

    /**
     * 计算儿童单价：套餐儿童价 + 日期附加费 + 批次附加费
     */
    private BigDecimal calculateChildUnitPrice(TourPackage tourPackage, TourBatch tourBatch) {
        BigDecimal childPrice = tourPackage.getChildPrice();
        if (childPrice == null || childPrice.compareTo(BigDecimal.ZERO) == 0) {
            // 如果没有儿童价，返回0
            return BigDecimal.ZERO;
        }
        BigDecimal price = childPrice;
        if (tourBatch.getChildDateExtraFee() != null) {
            price = price.add(tourBatch.getChildDateExtraFee());
        }
        return price;
    }

    private Set<Long> parseIdSet(String value) {
        if (!StringUtils.isNotBlank(value)) {
            return Set.of();
        }
        try {
            if (value.trim().startsWith("[")) {
                List<Long> ids = OBJECT_MAPPER.readValue(value, new TypeReference<List<Long>>() {});
                return ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            }
        } catch (Exception e) {
            logger.debug("Parse batch id list JSON failed: {}", e.getMessage());
        }
        return java.util.Arrays.stream(value.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(text -> {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private record AddonPriceResult(BigDecimal totalAmount, Long primaryAddonId, String summary, String itemsJson) {
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
        return "TR" + dateStr + randomStr;
    }

    /**
     * 根据行程ID和出发日期获取批次ID
     */
    private Long getBatchId(Long tourId, LocalDate departureDate) {
        LambdaQueryWrapper<TourBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourBatch::getTourId, tourId)
               .eq(TourBatch::getDepartureDate, departureDate);
        TourBatch batch = tourBatchMapper.selectOne(wrapper);
        return batch != null ? batch.getId() : null;
    }

    /**
     * 支付订单
     */
    @Transactional
    public void payOrder(Long orderId, String paymentMethod) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!canAccessOrder(order, currentUser)) {
            throw new ServiceException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态不正确，无法支付");
        }

        // 查询批次ID
        Long batchId = getBatchId(order.getTourId(), order.getDepartureDate());
        if (batchId == null) {
            throw new ServiceException("批次不存在");
        }

        int totalPeople = order.getAdultCount() + (order.getChildCount() != null ? order.getChildCount() : 0);

        // 原子确认库存（支付时：将occupied转为remaining扣减）
        int result = tourBatchMapper.confirmOccupancy(batchId, totalPeople);
        if (result == 0) {
            logger.error("支付确认库存失败：订单号={}, 批次={}, 人数={}", order.getOrderNo(), batchId, totalPeople);
            throw new ServiceException("库存确认失败，请稍后重试");
        }

        order.setStatus(1); // 已支付
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        couponService.markUsed(order.getCouponUserId(), order.getId(), order.getOrderNo());
        tourOrderNotificationService.notifyPaymentSuccess(order);
        logger.info("行程订单支付成功：订单号={}", order.getOrderNo());
    }

    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!canAccessOrder(order, currentUser)) {
            throw new ServiceException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付的订单可以取消");
        }

        // 查询批次ID
        Long batchId = getBatchId(order.getTourId(), order.getDepartureDate());
        if (batchId != null) {
            int totalPeople = order.getAdultCount() + (order.getChildCount() != null ? order.getChildCount() : 0);
            // 原子释放锁定库存
            tourBatchMapper.releaseOccupancy(batchId, totalPeople);
            logger.info("释放锁定库存：批次={}, 人数={}", batchId, totalPeople);
        }

        order.setStatus(2); // 已取消
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        couponService.releaseLocked(order.getCouponUserId());
        siteNotificationService.sendToUser(order.getUserId(), "订单已取消",
                "订单 " + order.getOrderNo() + " 已取消，如需出行可重新下单。",
                "ORDER", "TOUR_ORDER", String.valueOf(order.getId()), "/orders");
        logger.info("行程订单取消成功：订单号={}", order.getOrderNo());
    }

    /**
     * 退款订单
     */
    @Transactional
    public void refundOrder(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!canManageOrders(currentUser)) {
            throw new ServiceException("权限不足，请联系管理员");
        }

        if (order.getStatus() != 1) {
            throw new ServiceException("只有已支付的订单可以退款");
        }

        // 查询批次
        LambdaQueryWrapper<TourBatch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(TourBatch::getTourId, order.getTourId())
                   .eq(TourBatch::getDepartureDate, order.getDepartureDate());
        TourBatch batch = tourBatchMapper.selectOne(batchWrapper);

        if (batch != null) {
            int totalPeople = order.getAdultCount() + (order.getChildCount() != null ? order.getChildCount() : 0);
            int maxCapacity = batch.getMaxCapacity() != null ? batch.getMaxCapacity() : 999;
            // 原子退还余位
            tourBatchMapper.returnRemaining(batch.getId(), totalPeople, maxCapacity);
            logger.info("退还余位：批次={}, 人数={}", batch.getId(), totalPeople);
        }

        order.setStatus(3); // 已退款
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        siteNotificationService.sendToUser(order.getUserId(), "订单已退款",
                "订单 " + order.getOrderNo() + " 已完成退款处理，请留意原支付账户到账情况。",
                "ORDER", "TOUR_ORDER", String.valueOf(order.getId()), "/orders");
        logger.info("行程订单退款成功：订单号={}", order.getOrderNo());
    }

    /**
     * 获取用户订单列表
     */
    public Page<TourOrder> getUserOrders(Integer status, Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }

        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourOrder::getUserId, currentUser.getId());

        if (status != null) {
            wrapper.eq(TourOrder::getStatus, status);
        }

        wrapper.orderByDesc(TourOrder::getCreateTime);

        Page<TourOrder> page = tourOrderMapper.selectPage(new Page<>(currentPage, size), wrapper);
        page.setRecords(page.getRecords().stream().map(this::copyMaskedOrderForUser).toList());
        return page;
    }

    /**
     * 获取订单详情
     */
    public TourOrder getOrderDetail(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!canAccessOrder(order, currentUser)) {
            throw new ServiceException("无权查看此订单");
        }

        return canManageOrders(currentUser) ? order : copyMaskedOrderForUser(order);
    }

    /**
     * 获取待支付订单联系人编辑信息，仅用于用户完善订单表单回填。
     */
    public Map<String, Object> getOrderContactForEdit(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!canAccessOrder(order, currentUser)) {
            throw new ServiceException("无权查看此订单");
        }
        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付订单可以编辑联系人信息");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("contactName", order.getContactName());
        data.put("contactPhone", order.getContactPhone());
        return data;
    }

    /**
     * 管理员获取所有订单
     */
    public Page<TourOrder> getAllOrders(String orderNo, String contactName, String contactPhone,
                                        Integer status, Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!canManageOrders(currentUser)) {
            throw new ServiceException("权限不足，请联系管理员");
        }
        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(orderNo)) {
            wrapper.like(TourOrder::getOrderNo, orderNo);
        }
        if (StringUtils.isNotBlank(contactName)) {
            wrapper.like(TourOrder::getContactName, contactName);
        }
        if (StringUtils.isNotBlank(contactPhone)) {
            wrapper.like(TourOrder::getContactPhone, contactPhone);
        }
        if (status != null) {
            wrapper.eq(TourOrder::getStatus, status);
        }

        wrapper.orderByDesc(TourOrder::getCreateTime);

        return tourOrderMapper.selectPage(new Page<>(currentPage, size), wrapper);
    }

    /**
     * 获取用户订单统计信息
     */
    public Map<String, Object> getUserOrderStats(Long userId) {
        LambdaQueryWrapper<TourOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourOrder::getUserId, userId);
        
        List<TourOrder> allOrders = tourOrderMapper.selectList(queryWrapper);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allOrders.size());
        stats.put("pending", 0);
        stats.put("paid", 0);
        stats.put("cancelled", 0);
        stats.put("refunded", 0);
        stats.put("completed", 0);
        
        for (TourOrder order : allOrders) {
            switch (order.getStatus()) {
                case 0:
                    stats.put("pending", (Integer) stats.get("pending") + 1);
                    break;
                case 1:
                    stats.put("paid", (Integer) stats.get("paid") + 1);
                    break;
                case 2:
                    stats.put("cancelled", (Integer) stats.get("cancelled") + 1);
                    break;
                case 3:
                    stats.put("refunded", (Integer) stats.get("refunded") + 1);
                    break;
                case 4:
                    stats.put("completed", (Integer) stats.get("completed") + 1);
                    break;
            }
        }
        
        return stats;
    }

    /**
     * 删除订单（管理员）
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!canManageOrders(currentUser)) {
            throw new ServiceException("权限不足，请联系管理员");
        }
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        // 只有已取消、已退款、已完成的订单可以删除
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != 4) {
            throw new ServiceException("只有已取消、已退款、已完成的订单可以删除");
        }

        tourOrderMapper.deleteById(orderId);
        logger.info("行程订单删除成功：订单号={}", order.getOrderNo());
    }

    /**
     * 更新订单联系人信息
     */
    @Transactional
    public void updateContactInfo(Long orderId, String contactName, String contactPhone) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!canAccessOrder(order, currentUser)) {
            throw new ServiceException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付的订单可以修改联系人信息");
        }

        order.setContactName(contactName);
        order.setContactPhone(contactPhone);
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        logger.info("订单联系人信息更新成功：订单号={}", order.getOrderNo());
    }

    /**
     * 检查用户是否有该行程的未支付订单
     */
    public TourOrder getPendingOrderByProductId(String productId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        // 根据行程编号查询行程
        LambdaQueryWrapper<Tour> tourWrapper = new LambdaQueryWrapper<>();
        tourWrapper.eq(Tour::getCode, productId);
        Tour tour = tourMapper.selectOne(tourWrapper);
        if (tour == null) {
            return null;
        }

        // 检查是否有未支付的订单
        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourOrder::getUserId, currentUser.getId())
               .eq(TourOrder::getTourId, tour.getId())
               .eq(TourOrder::getStatus, 0); // 待支付状态
        TourOrder pendingOrder = tourOrderMapper.selectOne(wrapper);
        return pendingOrder == null ? null : copyMaskedOrderForUser(pendingOrder);
    }

    private TourOrder copyMaskedOrderForUser(TourOrder source) {
        if (source == null) {
            return null;
        }
        TourOrder copy = new TourOrder();
        copy.setId(source.getId());
        copy.setOrderNo(source.getOrderNo());
        copy.setUserId(source.getUserId());
        copy.setTourId(source.getTourId());
        copy.setTourName(source.getTourName());
        copy.setTourCode(source.getTourCode());
        copy.setPackageId(source.getPackageId());
        copy.setPackageName(source.getPackageName());
        copy.setBatchPackageId(source.getBatchPackageId());
        copy.setBatchPackageName(source.getBatchPackageName());
        copy.setAddonItems(source.getAddonItems());
        copy.setAddonSummary(source.getAddonSummary());
        copy.setDepartureDate(source.getDepartureDate());
        copy.setAdultCount(source.getAdultCount());
        copy.setChildCount(source.getChildCount());
        copy.setAdultUnitPrice(source.getAdultUnitPrice());
        copy.setChildUnitPrice(source.getChildUnitPrice());
        copy.setTourAmount(source.getTourAmount());
        copy.setHotelId(source.getHotelId());
        copy.setHotelName(source.getHotelName());
        copy.setHotelDays(source.getHotelDays());
        copy.setHotelPricePerNight(source.getHotelPricePerNight());
        copy.setHotelAmount(source.getHotelAmount());
        copy.setCouponUserId(source.getCouponUserId());
        copy.setCouponName(source.getCouponName());
        copy.setDiscountAmount(source.getDiscountAmount());
        copy.setPayableAmount(source.getPayableAmount());
        copy.setTotalAmount(source.getTotalAmount());
        copy.setContactName(source.getContactName());
        copy.setContactPhone(maskPhoneForUser(source.getContactPhone()));
        copy.setStatus(source.getStatus());
        copy.setPaymentMethod(source.getPaymentMethod());
        copy.setPaymentTime(source.getPaymentTime());
        copy.setRemark(source.getRemark());
        copy.setCreateTime(source.getCreateTime());
        copy.setUpdateTime(source.getUpdateTime());
        return copy;
    }

    private boolean canAccessOrder(TourOrder order, User currentUser) {
        if (order == null || currentUser == null) {
            return false;
        }
        return order.getUserId().equals(currentUser.getId()) || canManageOrders(currentUser);
    }

    private boolean canManageOrders(User currentUser) {
        return RolePermission.isAdmin(currentUser)
                && (RolePermission.isSuperAdmin(currentUser)
                || adminPermissionService.hasPermission(currentUser, "order:manage"));
    }

    private String maskPhoneForUser(String phone) {
        if (StringUtils.isBlank(phone)) {
            return phone;
        }
        String value = phone.trim();
        if (value.matches("^1[3-9]\\d{9}$")) {
            return value.replaceAll("^(\\d{3})\\d{4}(\\d{4})$", "$1****$2");
        }
        if (value.length() <= 4) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
