package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Coupon;
import org.example.springboot.entity.CouponUser;
import org.example.springboot.entity.Tour;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.entity.Traveler;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CouponMapper;
import org.example.springboot.mapper.CouponUserMapper;
import org.example.springboot.mapper.TourOrderMapper;
import org.example.springboot.mapper.TourMapper;
import org.example.springboot.mapper.TourPackageMapper;
import org.example.springboot.mapper.TravelerMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CouponService {
    public static final int USER_COUPON_UNUSED = 0;
    public static final int USER_COUPON_LOCKED = 1;
    public static final int USER_COUPON_USED = 2;
    public static final int USER_COUPON_EXPIRED = 3;
    public static final int USER_COUPON_VOID = 4;
    private static final String DISCOUNT_TYPE_AMOUNT = "AMOUNT";
    private static final String DISCOUNT_TYPE_RATE = "RATE";
    private static final String DISCOUNT_TYPE_AGE_GROUP_AMOUNT = "AGE_GROUP_AMOUNT";
    private static final String DISCOUNT_TYPE_AGE_GROUP_RATE = "AGE_GROUP_RATE";
    private static final String LEGACY_DISCOUNT_TYPE_AGE_PER_PERSON = "AGE_PER_PERSON";

    @Resource
    private CouponMapper couponMapper;
    @Resource
    private CouponUserMapper couponUserMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TourMapper tourMapper;
    @Resource
    private TourPackageMapper tourPackageMapper;
    @Resource
    private TourOrderMapper tourOrderMapper;
    @Resource
    private TravelerMapper travelerMapper;
    @Resource
    private SiteNotificationService siteNotificationService;
    @Resource
    private AdminPermissionService adminPermissionService;

    public Page<Coupon> pageCoupons(Integer currentPage, Integer pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), Coupon::getName, keyword)
                .eq(Coupon::getDeleted, 0)
                .eq(status != null, Coupon::getStatus, status)
                .orderByDesc(Coupon::getCreateTime);
        return couponMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);
    }

    @Transactional
    public void saveCoupon(Coupon coupon) {
        normalizeCoupon(coupon);
        if (coupon.getId() == null) {
            coupon.setIssuedQuantity(0);
            coupon.setUsedQuantity(0);
            coupon.setCreateTime(LocalDateTime.now());
            couponMapper.insert(coupon);
        } else {
            coupon.setUpdateTime(LocalDateTime.now());
            couponMapper.updateById(coupon);
        }
    }

    public void updateStatus(Long id, Integer status) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null || Integer.valueOf(1).equals(coupon.getDeleted())) throw new ServiceException("优惠券不存在");
        coupon.setStatus(status == null ? 0 : status);
        coupon.setUpdateTime(LocalDateTime.now());
        couponMapper.updateById(coupon);
        if (!Integer.valueOf(1).equals(coupon.getStatus())) {
            voidUnusedCoupons(coupon.getId());
        }
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = requireCoupon(id);
        couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, id)
                .set(Coupon::getDeleted, 1)
                .set(Coupon::getStatus, 0)
                .set(Coupon::getUpdateTime, LocalDateTime.now()));
        voidUnusedCoupons(coupon.getId());
    }

    @Transactional
    public int issueToUsers(Long couponId, List<Long> userIds) {
        Coupon coupon = requireCoupon(couponId);
        if (userIds == null || userIds.isEmpty()) {
            throw new ServiceException("请选择发放用户");
        }
        int count = 0;
        for (Long userId : userIds.stream().filter(Objects::nonNull).distinct().toList()) {
            if (issueOne(coupon, userId, true) != null) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int issueToAllUsers(Long couponId) {
        Coupon coupon = requireCoupon(couponId);
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1))
                .stream().map(User::getId).toList();
        return issueToUsers(coupon.getId(), userIds);
    }

    @Transactional
    public CouponUser receive(Long couponId) {
        User user = requireUser();
        Coupon coupon = requireCoupon(couponId);
        CouponUser couponUser = issueOne(coupon, user.getId(), false);
        if (couponUser == null) {
            throw new ServiceException("领取失败，请稍后再试");
        }
        return couponUser;
    }

    public List<Coupon> listReceivable() {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .eq(Coupon::getDeleted, 0)
                .eq(Coupon::getAutoReceive, 1)
                .and(w -> w.isNull(Coupon::getReceiveStartTime).or().le(Coupon::getReceiveStartTime, now))
                .and(w -> w.isNull(Coupon::getReceiveEndTime).or().ge(Coupon::getReceiveEndTime, now))
                .and(w -> w.isNull(Coupon::getValidEndTime).or().ge(Coupon::getValidEndTime, now))
                .orderByAsc(Coupon::getMinOrderAmount)
                .orderByDesc(Coupon::getDiscountAmount));
    }

    public List<CouponUser> myCoupons(Integer status) {
        User user = requireUser();
        refreshExpired(user.getId());
        LambdaQueryWrapper<CouponUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponUser::getUserId, user.getId())
                .eq(status != null, CouponUser::getStatus, status)
                .orderByAsc(CouponUser::getStatus)
                .orderByAsc(CouponUser::getValidEndTime);
        return couponUserMapper.selectList(wrapper);
    }

    public List<CouponUser> availableForOrder(Long tourId, Long packageId, BigDecimal orderAmount) {
        User user = requireUser();
        Tour tour = tourMapper.selectById(tourId);
        if (tour == null) return List.of();
        org.example.springboot.entity.TourPackage tourPackage = packageId == null ? null : tourPackageMapper.selectById(packageId);
        refreshExpired(user.getId());
        return couponUserMapper.selectList(new LambdaQueryWrapper<CouponUser>()
                        .eq(CouponUser::getUserId, user.getId())
                        .eq(CouponUser::getStatus, USER_COUPON_UNUSED)
                        .and(w -> w.isNull(CouponUser::getValidStartTime).or().le(CouponUser::getValidStartTime, LocalDateTime.now()))
                        .and(w -> w.isNull(CouponUser::getValidEndTime).or().ge(CouponUser::getValidEndTime, LocalDateTime.now()))
                        .orderByAsc(CouponUser::getValidEndTime))
                .stream()
                .filter(item -> isTemplateActive(item.getCouponId()))
                .filter(item -> !hasAgeLimit(item) && !isLegacyAgeGroupCouponType(item.getDiscountType()))
                .filter(item -> calculateDiscount(item, tour, tourPackage, orderAmount).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    public List<CouponUser> availableForOrder(Long orderId, List<Traveler> travelers) {
        User user = requireUser();
        TourOrder order = requirePendingOrderAccess(orderId, user);
        Tour tour = tourMapper.selectById(order.getTourId());
        if (tour == null) return List.of();
        org.example.springboot.entity.TourPackage tourPackage = tourPackageMapper.selectById(order.getPackageId());
        List<Traveler> effectiveTravelers = travelers == null || travelers.isEmpty()
                ? listTravelers(order.getId())
                : travelers;
        if (!isTravelerCountMatched(order, effectiveTravelers)) {
            return List.of();
        }
        refreshExpired(user.getId());
        BigDecimal orderAmount = grossOrderAmount(order);
        List<CouponUser> availableCoupons = new ArrayList<>(couponUserMapper.selectList(new LambdaQueryWrapper<CouponUser>()
                        .eq(CouponUser::getUserId, user.getId())
                        .eq(CouponUser::getStatus, USER_COUPON_UNUSED)
                        .and(w -> w.isNull(CouponUser::getValidStartTime).or().le(CouponUser::getValidStartTime, LocalDateTime.now()))
                        .and(w -> w.isNull(CouponUser::getValidEndTime).or().ge(CouponUser::getValidEndTime, LocalDateTime.now()))
                        .orderByAsc(CouponUser::getValidEndTime))
                .stream()
                .filter(item -> isTemplateActive(item.getCouponId()))
                .filter(item -> calculateDiscount(item, tour, tourPackage, order.getDepartureDate(), effectiveTravelers, order, orderAmount).compareTo(BigDecimal.ZERO) > 0)
                .toList());
        if (order.getCouponUserId() != null && availableCoupons.stream().noneMatch(item -> Objects.equals(item.getId(), order.getCouponUserId()))) {
            CouponUser lockedCoupon = couponUserMapper.selectById(order.getCouponUserId());
            if (lockedCoupon != null
                    && Objects.equals(lockedCoupon.getUserId(), user.getId())
                    && Integer.valueOf(USER_COUPON_LOCKED).equals(lockedCoupon.getStatus())
                    && Objects.equals(lockedCoupon.getOrderId(), order.getId())
                    && calculateDiscount(lockedCoupon, tour, tourPackage, order.getDepartureDate(), effectiveTravelers, order, orderAmount).compareTo(BigDecimal.ZERO) > 0) {
                availableCoupons.add(0, lockedCoupon);
            }
        }
        availableCoupons.sort(Comparator.comparing(
                (CouponUser item) -> calculateDiscount(item, tour, tourPackage, order.getDepartureDate(), effectiveTravelers, order, orderAmount)
        ).reversed());
        return availableCoupons;
    }

    @Transactional
    public CouponUseResult lockForOrder(Long couponUserId, Tour tour, BigDecimal orderAmount, Long orderId, String orderNo) {
        return lockForOrder(couponUserId, tour, null, orderAmount, orderId, orderNo);
    }

    @Transactional
    public CouponUseResult lockForOrder(Long couponUserId, Tour tour, org.example.springboot.entity.TourPackage tourPackage, BigDecimal orderAmount, Long orderId, String orderNo) {
        return lockForOrder(couponUserId, tour, tourPackage, null, List.of(), orderAmount, orderId, orderNo);
    }

    @Transactional
    public CouponUseResult lockForOrder(Long couponUserId, Tour tour, org.example.springboot.entity.TourPackage tourPackage,
                                        LocalDate departureDate, List<Traveler> travelers, BigDecimal orderAmount,
                                        Long orderId, String orderNo) {
        if (couponUserId == null) {
            return new CouponUseResult(null, "", BigDecimal.ZERO);
        }
        User user = requireUser();
        CouponUser couponUser = couponUserMapper.selectById(couponUserId);
        if (couponUser == null || !couponUser.getUserId().equals(user.getId())) {
            throw new ServiceException("优惠券不存在");
        }
        boolean lockedForSameOrder = Integer.valueOf(USER_COUPON_LOCKED).equals(couponUser.getStatus())
                && orderId != null
                && Objects.equals(couponUser.getOrderId(), orderId);
        if (!Integer.valueOf(USER_COUPON_UNUSED).equals(couponUser.getStatus()) && !lockedForSameOrder) {
            throw new ServiceException("该优惠券不可用");
        }
        Coupon template = couponMapper.selectById(couponUser.getCouponId());
        if (!lockedForSameOrder && !isTemplateActive(template)) {
            couponUser.setStatus(USER_COUPON_VOID);
            couponUserMapper.updateById(couponUser);
            throw new ServiceException("优惠券已失效");
        }
        TourOrder order = orderId == null ? null : tourOrderMapper.selectById(orderId);
        BigDecimal discount = calculateDiscount(couponUser, tour, tourPackage, departureDate, travelers, order, orderAmount);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("优惠券不满足使用条件");
        }
        couponUser.setStatus(USER_COUPON_LOCKED);
        couponUser.setOrderId(orderId);
        couponUser.setOrderNo(orderNo);
        couponUser.setUsedAmount(discount);
        couponUser.setLockTime(LocalDateTime.now());
        couponUserMapper.updateById(couponUser);
        return new CouponUseResult(couponUser.getId(), couponUser.getCouponName(), discount);
    }

    @Transactional
    public TourOrder applyCouponToOrder(Long orderId, Long couponUserId) {
        User user = requireUser();
        TourOrder order = requirePendingOrderAccess(orderId, user);
        List<Traveler> travelers = listTravelers(order.getId());
        if (travelers.isEmpty()) {
            throw new ServiceException("请先填写出行人信息后再选择优惠券");
        }
        if (!isTravelerCountMatched(order, travelers)) {
            throw new ServiceException("出行人数量与订单人数不一致，请重新填写出行人信息");
        }
        Tour tour = tourMapper.selectById(order.getTourId());
        org.example.springboot.entity.TourPackage tourPackage = tourPackageMapper.selectById(order.getPackageId());
        BigDecimal grossAmount = grossOrderAmount(order);

        Long oldCouponUserId = order.getCouponUserId();
        if (oldCouponUserId != null && !Objects.equals(oldCouponUserId, couponUserId)) {
            releaseLocked(oldCouponUserId);
            order.setCouponUserId(null);
            order.setCouponName(null);
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        CouponUseResult result = couponUserId == null
                ? new CouponUseResult(null, "", BigDecimal.ZERO)
                : lockForOrder(couponUserId, tour, tourPackage, order.getDepartureDate(), travelers, grossAmount, order.getId(), order.getOrderNo());
        BigDecimal discount = result.discountAmount();
        BigDecimal payable = grossAmount.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        order.setCouponUserId(result.couponUserId());
        order.setCouponName(StringUtils.defaultIfBlank(result.couponName(), null));
        order.setDiscountAmount(discount);
        order.setPayableAmount(payable);
        order.setTotalAmount(payable);
        order.setUpdateTime(LocalDateTime.now());
        tourOrderMapper.updateById(order);
        return order;
    }

    @Transactional
    public void revalidateOrderCoupon(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null || order.getCouponUserId() == null || !Integer.valueOf(0).equals(order.getStatus())) {
            return;
        }
        CouponUser couponUser = couponUserMapper.selectById(order.getCouponUserId());
        if (couponUser == null || !Integer.valueOf(USER_COUPON_LOCKED).equals(couponUser.getStatus())) {
            clearOrderCoupon(order);
            return;
        }
        List<Traveler> travelers = listTravelers(order.getId());
        if (!isTravelerCountMatched(order, travelers)) {
            releaseLocked(couponUser.getId());
            clearOrderCoupon(order);
            return;
        }
        Tour tour = tourMapper.selectById(order.getTourId());
        org.example.springboot.entity.TourPackage tourPackage = tourPackageMapper.selectById(order.getPackageId());
        BigDecimal grossAmount = grossOrderAmount(order);
        BigDecimal discount = calculateDiscount(couponUser, tour, tourPackage, order.getDepartureDate(), travelers, order, grossAmount);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            releaseLocked(couponUser.getId());
            clearOrderCoupon(order);
            return;
        }
        BigDecimal payable = grossAmount.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        order.setDiscountAmount(discount);
        order.setPayableAmount(payable);
        order.setTotalAmount(payable);
        order.setUpdateTime(LocalDateTime.now());
        tourOrderMapper.updateById(order);
    }

    @Transactional
    public void markUsed(Long couponUserId, Long orderId, String orderNo) {
        if (couponUserId == null) return;
        CouponUser couponUser = couponUserMapper.selectById(couponUserId);
        if (couponUser == null) return;
        if (Integer.valueOf(USER_COUPON_USED).equals(couponUser.getStatus())) {
            return;
        }
        if (!Integer.valueOf(USER_COUPON_LOCKED).equals(couponUser.getStatus())
                || !Objects.equals(couponUser.getOrderId(), orderId)) {
            throw new ServiceException("优惠券未锁定到当前订单，无法核销");
        }
        int updated = couponUserMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                .eq(CouponUser::getId, couponUserId)
                .eq(CouponUser::getStatus, USER_COUPON_LOCKED)
                .eq(CouponUser::getOrderId, orderId)
                .set(CouponUser::getStatus, USER_COUPON_USED)
                .set(CouponUser::getOrderId, orderId)
                .set(CouponUser::getOrderNo, orderNo)
                .set(CouponUser::getUseTime, LocalDateTime.now()));
        if (updated > 0) {
            couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                    .eq(Coupon::getId, couponUser.getCouponId())
                    .setSql("used_quantity = used_quantity + 1"));
        }
    }

    @Transactional
    public void bindLockedOrder(Long couponUserId, Long orderId, String orderNo) {
        if (couponUserId == null) return;
        CouponUser couponUser = couponUserMapper.selectById(couponUserId);
        if (couponUser == null || !Integer.valueOf(USER_COUPON_LOCKED).equals(couponUser.getStatus())) return;
        couponUserMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                .eq(CouponUser::getId, couponUserId)
                .eq(CouponUser::getStatus, USER_COUPON_LOCKED)
                .set(CouponUser::getOrderId, orderId)
                .set(CouponUser::getOrderNo, orderNo));
    }

    @Transactional
    public void releaseLocked(Long couponUserId) {
        if (couponUserId == null) return;
        CouponUser couponUser = couponUserMapper.selectById(couponUserId);
        if (couponUser == null || !Integer.valueOf(USER_COUPON_LOCKED).equals(couponUser.getStatus())) return;
        int nextStatus = isExpired(couponUser.getValidEndTime())
                ? USER_COUPON_EXPIRED
                : (isTemplateActive(couponUser.getCouponId()) ? USER_COUPON_UNUSED : USER_COUPON_VOID);
        couponUserMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                .eq(CouponUser::getId, couponUserId)
                .eq(CouponUser::getStatus, USER_COUPON_LOCKED)
                .set(CouponUser::getStatus, nextStatus)
                .set(CouponUser::getOrderId, null)
                .set(CouponUser::getOrderNo, null)
                .set(CouponUser::getUsedAmount, null)
                .set(CouponUser::getLockTime, null));
    }

    public BigDecimal calculateDiscount(CouponUser couponUser, Tour tour, BigDecimal orderAmount) {
        return calculateDiscount(couponUser, tour, null, orderAmount);
    }

    public BigDecimal calculateDiscount(CouponUser couponUser, Tour tour, org.example.springboot.entity.TourPackage tourPackage, BigDecimal orderAmount) {
        return calculateDiscount(couponUser, tour, tourPackage, null, List.of(), orderAmount);
    }

    private CouponUser issueOne(Coupon coupon, Long userId, boolean byAdmin) {
        validateCanIssue(coupon);
        User targetUser = userMapper.selectById(userId);
        if (targetUser == null || !Integer.valueOf(1).equals(targetUser.getStatus())) {
            if (byAdmin) return null;
            throw new ServiceException("用户不存在或已停用");
        }
        long owned = couponUserMapper.selectCount(new LambdaQueryWrapper<CouponUser>()
                .eq(CouponUser::getCouponId, coupon.getId())
                .eq(CouponUser::getUserId, userId));
        if (owned >= coupon.getPerUserLimit()) {
            if (byAdmin) return null;
            throw new ServiceException("你已领取过该优惠券");
        }
        if (coupon.getTotalQuantity() != null && coupon.getTotalQuantity() > 0
                && coupon.getIssuedQuantity() != null && coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new ServiceException("优惠券已发完");
        }
        int issued = couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, coupon.getId())
                .eq(Coupon::getStatus, 1)
                .apply("(total_quantity = 0 OR issued_quantity < total_quantity)")
                .setSql("issued_quantity = issued_quantity + 1")
                .setSql("status = CASE WHEN total_quantity > 0 AND issued_quantity + 1 >= total_quantity THEN 0 ELSE status END"));
        if (issued <= 0) {
            if (byAdmin) return null;
            throw new ServiceException("优惠券已发完");
        }
        CouponUser item = snapshot(coupon, userId);
        couponUserMapper.insert(item);
        siteNotificationService.sendToUser(
                userId,
                "你收到一张优惠券",
                "优惠券「" + coupon.getName() + "」已发放到账户，可在预订行程时使用。",
                "COUPON",
                "COUPON_USER",
                String.valueOf(item.getId()),
                "/profile?tab=coupons"
        );
        return item;
    }

    private CouponUser snapshot(Coupon coupon, Long userId) {
        CouponUser item = new CouponUser();
        item.setCouponId(coupon.getId());
        item.setUserId(userId);
        item.setCouponName(coupon.getName());
        item.setCouponCode(coupon.getCode());
        item.setDiscountType(coupon.getDiscountType());
        item.setDiscountAmount(coupon.getDiscountAmount());
        item.setDiscountRate(coupon.getDiscountRate());
        item.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        item.setMinOrderAmount(coupon.getMinOrderAmount());
        item.setMinAge(coupon.getMinAge());
        item.setMaxAge(coupon.getMaxAge());
        item.setScopeType(coupon.getScopeType());
        item.setScopeIds(coupon.getScopeIds());
        item.setValidStartTime(coupon.getValidStartTime());
        item.setValidEndTime(coupon.getValidEndTime());
        item.setStatus(USER_COUPON_UNUSED);
        item.setReceiveTime(LocalDateTime.now());
        item.setCreateTime(LocalDateTime.now());
        return item;
    }

    private void normalizeCoupon(Coupon coupon) {
        if (!StringUtils.isNotBlank(coupon.getName())) throw new ServiceException("请输入优惠券名称");
        if (!StringUtils.isNotBlank(coupon.getCode())) {
            coupon.setCode("CP" + System.currentTimeMillis());
        }
        if (isAgeAmountType(coupon.getDiscountType())) {
            coupon.setDiscountType(DISCOUNT_TYPE_AMOUNT);
            if (zero(coupon.getDiscountAmount()).compareTo(BigDecimal.ZERO) <= 0) throw new ServiceException("请输入每人减免金额");
        } else if (isAgeRateType(coupon.getDiscountType())) {
            coupon.setDiscountType(DISCOUNT_TYPE_RATE);
            if (coupon.getDiscountRate() == null || coupon.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 || coupon.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
                throw new ServiceException("年龄专享折扣比例应大于0且小于1");
            }
        } else if (!DISCOUNT_TYPE_RATE.equalsIgnoreCase(coupon.getDiscountType())) {
            coupon.setDiscountType(DISCOUNT_TYPE_AMOUNT);
            if (zero(coupon.getDiscountAmount()).compareTo(BigDecimal.ZERO) <= 0) throw new ServiceException("请输入优惠金额");
        } else if (coupon.getDiscountRate() == null || coupon.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 || coupon.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
            coupon.setDiscountType(DISCOUNT_TYPE_RATE);
            throw new ServiceException("折扣比例应大于0且小于1");
        }
        if (coupon.getValidStartTime() != null && coupon.getValidEndTime() != null && !coupon.getValidEndTime().isAfter(coupon.getValidStartTime())) {
            throw new ServiceException("请设置正确有效期");
        }
        coupon.setMinOrderAmount(zero(coupon.getMinOrderAmount()));
        if (coupon.getMinAge() != null && coupon.getMinAge() < 0) {
            throw new ServiceException("最低年龄不能小于0");
        }
        if (coupon.getMaxAge() != null && coupon.getMaxAge() < 0) {
            throw new ServiceException("最高年龄不能小于0");
        }
        if (coupon.getMinAge() != null && coupon.getMaxAge() != null && coupon.getMaxAge() < coupon.getMinAge()) {
            throw new ServiceException("最高年龄不能小于最低年龄");
        }
        coupon.setScopeType(StringUtils.defaultIfBlank(coupon.getScopeType(), "ALL_TOUR"));
        coupon.setTotalQuantity(coupon.getTotalQuantity() == null ? 0 : coupon.getTotalQuantity());
        coupon.setPerUserLimit(coupon.getPerUserLimit() == null || coupon.getPerUserLimit() < 1 ? 1 : coupon.getPerUserLimit());
        coupon.setIssuedQuantity(coupon.getIssuedQuantity() == null ? 0 : coupon.getIssuedQuantity());
        coupon.setUsedQuantity(coupon.getUsedQuantity() == null ? 0 : coupon.getUsedQuantity());
        coupon.setAutoReceive(coupon.getAutoReceive() == null ? 1 : coupon.getAutoReceive());
        coupon.setStackable(0);
        coupon.setStatus(coupon.getStatus() == null ? 1 : coupon.getStatus());
        coupon.setDeleted(coupon.getDeleted() == null ? 0 : coupon.getDeleted());
        coupon.setReceiveStartTime(null);
        coupon.setReceiveEndTime(null);
    }

    private void validateCanIssue(Coupon coupon) {
        if (!Integer.valueOf(1).equals(coupon.getStatus())) throw new ServiceException("优惠券已停用");
        if (Integer.valueOf(1).equals(coupon.getDeleted())) throw new ServiceException("优惠券已删除");
        if (isExpired(coupon.getValidEndTime())) throw new ServiceException("优惠券已过期");
    }

    private boolean isScopeMatched(String scopeType, String scopeIds, Tour tour, org.example.springboot.entity.TourPackage tourPackage, LocalDate departureDate) {
        if ("ALL_TOUR".equalsIgnoreCase(scopeType)) return true;
        Set<String> ids = Arrays.stream(StringUtils.defaultString(scopeIds).split("[,，、\\s]+"))
                .map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if ("TOUR".equalsIgnoreCase(scopeType)) {
            return ids.stream().anyMatch(value -> isTourScopeValueMatched(value, tour, departureDate));
        }
        if ("TOUR_PACKAGE".equalsIgnoreCase(scopeType)) {
            if (tourPackage == null) return false;
            return ids.stream().anyMatch(value -> isPackageScopeValueMatched(value, tour, tourPackage, departureDate));
        }
        if ("TOUR_TYPE".equalsIgnoreCase(scopeType)) return ids.contains(tour.getTourType());
        return false;
    }

    private boolean isTourScopeValueMatched(String value, Tour tour, LocalDate departureDate) {
        String[] parts = splitScopeDate(value);
        if (!isDateMatched(parts[1], departureDate)) return false;
        return Objects.equals(parts[0], String.valueOf(tour.getId())) || Objects.equals(parts[0], tour.getCode());
    }

    private boolean isPackageScopeValueMatched(String value, Tour tour, org.example.springboot.entity.TourPackage tourPackage, LocalDate departureDate) {
        String[] parts = splitScopeDate(value);
        if (!isDateMatched(parts[1], departureDate)) return false;
        String scope = parts[0];
        return Objects.equals(scope, tour.getId() + ":" + tourPackage.getId())
                || Objects.equals(scope, tour.getCode() + ":" + tourPackage.getId())
                || Objects.equals(scope, String.valueOf(tourPackage.getId()));
    }

    private String[] splitScopeDate(String value) {
        String text = StringUtils.defaultString(value).trim();
        int index = text.indexOf('@');
        if (index < 0) {
            return new String[]{text, ""};
        }
        return new String[]{text.substring(0, index).trim(), text.substring(index + 1).trim()};
    }

    private boolean isDateMatched(String dateText, LocalDate departureDate) {
        if (StringUtils.isBlank(dateText)) return true;
        return departureDate != null && Objects.equals(dateText, departureDate.toString());
    }

    public BigDecimal calculateDiscount(CouponUser couponUser, Tour tour, org.example.springboot.entity.TourPackage tourPackage,
                                        LocalDate departureDate, List<Traveler> travelers, BigDecimal orderAmount) {
        return calculateDiscount(couponUser, tour, tourPackage, departureDate, travelers, null, orderAmount);
    }

    public BigDecimal calculateDiscount(CouponUser couponUser, Tour tour, org.example.springboot.entity.TourPackage tourPackage,
                                        LocalDate departureDate, List<Traveler> travelers, TourOrder order, BigDecimal orderAmount) {
        if (couponUser == null || tour == null || orderAmount == null) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        if (couponUser.getValidStartTime() != null && couponUser.getValidStartTime().isAfter(now)) return BigDecimal.ZERO;
        if (isExpired(couponUser.getValidEndTime())) return BigDecimal.ZERO;
        boolean ageScoped = hasAgeLimit(couponUser);
        if (!ageScoped && orderAmount.compareTo(zero(couponUser.getMinOrderAmount())) < 0) return BigDecimal.ZERO;
        if (!isScopeMatched(couponUser.getScopeType(), couponUser.getScopeIds(), tour, tourPackage, departureDate)) return BigDecimal.ZERO;
        if (!isAgeMatched(couponUser, departureDate, travelers)) return BigDecimal.ZERO;
        BigDecimal discount;
        if (isRateType(couponUser.getDiscountType())) {
            BigDecimal rate = couponUser.getDiscountRate() == null ? BigDecimal.ONE : couponUser.getDiscountRate();
            discount = ageScoped
                    ? calculateAgeScopedRateDiscount(couponUser, departureDate, travelers, order)
                    : orderAmount.multiply(BigDecimal.ONE.subtract(rate));
            if (couponUser.getMaxDiscountAmount() != null && couponUser.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(couponUser.getMaxDiscountAmount());
            }
        } else if (isAmountType(couponUser.getDiscountType())) {
            discount = ageScoped
                    ? calculateAgeScopedAmountDiscount(couponUser, departureDate, travelers, order)
                    : zero(couponUser.getDiscountAmount());
        } else {
            discount = zero(couponUser.getDiscountAmount());
        }
        BigDecimal capped = discount.min(orderAmount);
        if (capped.compareTo(BigDecimal.ZERO) < 0) {
            capped = BigDecimal.ZERO;
        }
        return capped.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isAgeMatched(CouponUser couponUser, LocalDate departureDate, List<Traveler> travelers) {
        Integer minAge = couponUser.getMinAge();
        Integer maxAge = couponUser.getMaxAge();
        if (minAge == null && maxAge == null) return true;
        return countAgeMatchedTravelers(couponUser, departureDate, travelers) > 0;
    }

    private int countAgeMatchedTravelers(CouponUser couponUser, LocalDate departureDate, List<Traveler> travelers) {
        if (travelers == null || travelers.isEmpty()) return 0;
        Integer minAge = couponUser.getMinAge();
        Integer maxAge = couponUser.getMaxAge();
        LocalDate travelDate = departureDate == null ? LocalDate.now() : departureDate;
        return (int) travelers.stream()
                .filter(Objects::nonNull)
                .map(Traveler::getBirthDate)
                .filter(Objects::nonNull)
                .map(birthDate -> Period.between(birthDate, travelDate).getYears())
                .filter(age -> age >= 0)
                .filter(age -> (minAge == null || age >= minAge) && (maxAge == null || age <= maxAge))
                .count();
    }

    private BigDecimal calculateAgeScopedAmountDiscount(CouponUser couponUser, LocalDate departureDate, List<Traveler> travelers, TourOrder order) {
        BigDecimal perPersonDiscount = zero(couponUser.getDiscountAmount());
        if (perPersonDiscount.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return eligibleTravelerUnitPrices(couponUser, departureDate, travelers, order, true).stream()
                .map(unitPrice -> perPersonDiscount.min(unitPrice))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAgeScopedRateDiscount(CouponUser couponUser, LocalDate departureDate, List<Traveler> travelers, TourOrder order) {
        BigDecimal rate = couponUser.getDiscountRate() == null ? BigDecimal.ONE : couponUser.getDiscountRate();
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) return BigDecimal.ZERO;
        BigDecimal discountRate = BigDecimal.ONE.subtract(rate);
        return eligibleTravelerUnitPrices(couponUser, departureDate, travelers, order, true).stream()
                .map(unitPrice -> unitPrice.multiply(discountRate))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<BigDecimal> eligibleTravelerUnitPrices(CouponUser couponUser, LocalDate departureDate, List<Traveler> travelers, TourOrder order,
                                                        boolean requirePerTravelerThreshold) {
        if (order == null || travelers == null || travelers.isEmpty()) return List.of();
        LocalDate travelDate = departureDate == null ? LocalDate.now() : departureDate;
        BigDecimal minUnitPrice = requirePerTravelerThreshold ? zero(couponUser.getMinOrderAmount()) : BigDecimal.ZERO;
        return travelers.stream()
                .filter(Objects::nonNull)
                .filter(traveler -> traveler.getBirthDate() != null)
                .filter(traveler -> isAgeInRange(Period.between(traveler.getBirthDate(), travelDate).getYears(), couponUser))
                .map(traveler -> travelerUnitPrice(order, traveler))
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .filter(price -> price.compareTo(minUnitPrice) >= 0)
                .toList();
    }

    private boolean isAgeInRange(int age, CouponUser couponUser) {
        if (age < 0) return false;
        Integer minAge = couponUser.getMinAge();
        Integer maxAge = couponUser.getMaxAge();
        return (minAge == null || age >= minAge) && (maxAge == null || age <= maxAge);
    }

    private BigDecimal travelerUnitPrice(TourOrder order, Traveler traveler) {
        if ("CHILD".equalsIgnoreCase(traveler.getTravelerType())) {
            return zero(order.getChildUnitPrice());
        }
        return zero(order.getAdultUnitPrice());
    }

    private boolean hasAgeLimit(CouponUser couponUser) {
        return couponUser != null && (couponUser.getMinAge() != null || couponUser.getMaxAge() != null);
    }

    private boolean isAmountType(String discountType) {
        return DISCOUNT_TYPE_AMOUNT.equalsIgnoreCase(discountType) || isAgeAmountType(discountType);
    }

    private boolean isRateType(String discountType) {
        return DISCOUNT_TYPE_RATE.equalsIgnoreCase(discountType) || isAgeRateType(discountType);
    }

    private boolean isLegacyAgeGroupCouponType(String discountType) {
        return isAgeAmountType(discountType) || isAgeRateType(discountType);
    }

    private boolean isAgeRateType(String discountType) {
        return DISCOUNT_TYPE_AGE_GROUP_RATE.equalsIgnoreCase(discountType);
    }

    private boolean isAgeAmountType(String discountType) {
        return DISCOUNT_TYPE_AGE_GROUP_AMOUNT.equalsIgnoreCase(discountType)
                || LEGACY_DISCOUNT_TYPE_AGE_PER_PERSON.equalsIgnoreCase(discountType);
    }

    private List<Traveler> listTravelers(Long orderId) {
        if (orderId == null) return List.of();
        return travelerMapper.selectList(new LambdaQueryWrapper<Traveler>()
                .eq(Traveler::getOrderId, orderId)
                .orderByAsc(Traveler::getTravelerIndex));
    }

    private boolean isTravelerCountMatched(TourOrder order, List<Traveler> travelers) {
        if (order == null || travelers == null) return false;
        int adultCount = 0;
        int childCount = 0;
        for (Traveler traveler : travelers) {
            if (traveler == null) continue;
            if ("CHILD".equalsIgnoreCase(traveler.getTravelerType())) {
                childCount++;
            } else if ("ADULT".equalsIgnoreCase(traveler.getTravelerType())) {
                adultCount++;
            } else {
                return false;
            }
        }
        return adultCount == safeCount(order.getAdultCount()) && childCount == safeCount(order.getChildCount());
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private TourOrder requirePendingOrderAccess(Long orderId, User user) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) throw new ServiceException("订单不存在");
        if (!order.getUserId().equals(user.getId()) && !canManageOrders(user)) {
            throw new ServiceException("无权操作此订单");
        }
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            throw new ServiceException("只有待支付订单可以选择优惠券");
        }
        return order;
    }

    private boolean canManageOrders(User currentUser) {
        return RolePermission.isAdmin(currentUser)
                && (RolePermission.isSuperAdmin(currentUser)
                || adminPermissionService.hasPermission(currentUser, "order:manage"));
    }

    private BigDecimal grossOrderAmount(TourOrder order) {
        return zero(order.getTourAmount()).add(zero(order.getHotelAmount())).setScale(2, RoundingMode.HALF_UP);
    }

    private void clearOrderCoupon(TourOrder order) {
        BigDecimal grossAmount = grossOrderAmount(order);
        order.setCouponUserId(null);
        order.setCouponName(null);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayableAmount(grossAmount);
        order.setTotalAmount(grossAmount);
        order.setUpdateTime(LocalDateTime.now());
        tourOrderMapper.updateById(order);
    }

    private void refreshExpired(Long userId) {
        couponUserMapper.selectList(new LambdaQueryWrapper<CouponUser>()
                        .eq(CouponUser::getUserId, userId)
                        .in(CouponUser::getStatus, List.of(USER_COUPON_UNUSED, USER_COUPON_LOCKED))
                        .isNotNull(CouponUser::getValidEndTime)
                        .lt(CouponUser::getValidEndTime, LocalDateTime.now()))
                .forEach(item -> {
                    item.setStatus(USER_COUPON_EXPIRED);
                    couponUserMapper.updateById(item);
                });
    }

    private void voidUnusedCoupons(Long couponId) {
        couponUserMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                .eq(CouponUser::getCouponId, couponId)
                .eq(CouponUser::getStatus, USER_COUPON_UNUSED)
                .set(CouponUser::getStatus, USER_COUPON_VOID)
                .set(CouponUser::getUsedAmount, null)
                .set(CouponUser::getLockTime, null));
    }

    private boolean isTemplateActive(Long couponId) {
        Coupon coupon = couponId == null ? null : couponMapper.selectById(couponId);
        return isTemplateActive(coupon);
    }

    private boolean isTemplateActive(Coupon coupon) {
        return coupon != null && Integer.valueOf(1).equals(coupon.getStatus()) && !Integer.valueOf(1).equals(coupon.getDeleted());
    }

    private Coupon requireCoupon(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null || Integer.valueOf(1).equals(coupon.getDeleted())) throw new ServiceException("优惠券不存在");
        return coupon;
    }

    private boolean isExpired(LocalDateTime validEndTime) {
        return validEndTime != null && validEndTime.isBefore(LocalDateTime.now());
    }

    private User requireUser() {
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) throw new ServiceException("请先登录");
        return user;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record CouponUseResult(Long couponUserId, String couponName, BigDecimal discountAmount) {
    }
}
