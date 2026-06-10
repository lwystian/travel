package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Coupon;
import org.example.springboot.entity.CouponUser;
import org.example.springboot.entity.Tour;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CouponMapper;
import org.example.springboot.mapper.CouponUserMapper;
import org.example.springboot.mapper.TourMapper;
import org.example.springboot.mapper.TourPackageMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
    private SiteNotificationService siteNotificationService;

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
                .filter(item -> calculateDiscount(item, tour, tourPackage, orderAmount).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Transactional
    public CouponUseResult lockForOrder(Long couponUserId, Tour tour, BigDecimal orderAmount, Long orderId, String orderNo) {
        return lockForOrder(couponUserId, tour, null, orderAmount, orderId, orderNo);
    }

    @Transactional
    public CouponUseResult lockForOrder(Long couponUserId, Tour tour, org.example.springboot.entity.TourPackage tourPackage, BigDecimal orderAmount, Long orderId, String orderNo) {
        if (couponUserId == null) {
            return new CouponUseResult(null, "", BigDecimal.ZERO);
        }
        User user = requireUser();
        CouponUser couponUser = couponUserMapper.selectById(couponUserId);
        if (couponUser == null || !couponUser.getUserId().equals(user.getId())) {
            throw new ServiceException("优惠券不存在");
        }
        if (!Integer.valueOf(USER_COUPON_UNUSED).equals(couponUser.getStatus())) {
            throw new ServiceException("该优惠券不可用");
        }
        Coupon template = couponMapper.selectById(couponUser.getCouponId());
        if (template == null || !Integer.valueOf(1).equals(template.getStatus()) || Integer.valueOf(1).equals(template.getDeleted())) {
            couponUser.setStatus(USER_COUPON_VOID);
            couponUserMapper.updateById(couponUser);
            throw new ServiceException("优惠券已失效");
        }
        BigDecimal discount = calculateDiscount(couponUser, tour, tourPackage, orderAmount);
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
    public void markUsed(Long couponUserId, Long orderId, String orderNo) {
        if (couponUserId == null) return;
        CouponUser couponUser = couponUserMapper.selectById(couponUserId);
        if (couponUser == null) return;
        if (Integer.valueOf(USER_COUPON_USED).equals(couponUser.getStatus())) {
            return;
        }
        int updated = couponUserMapper.update(null, new LambdaUpdateWrapper<CouponUser>()
                .eq(CouponUser::getId, couponUserId)
                .ne(CouponUser::getStatus, USER_COUPON_USED)
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
        int nextStatus = isExpired(couponUser.getValidEndTime()) ? USER_COUPON_EXPIRED : USER_COUPON_UNUSED;
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
        if (couponUser == null || tour == null || orderAmount == null) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        if (couponUser.getValidStartTime() != null && couponUser.getValidStartTime().isAfter(now)) return BigDecimal.ZERO;
        if (isExpired(couponUser.getValidEndTime())) return BigDecimal.ZERO;
        if (orderAmount.compareTo(zero(couponUser.getMinOrderAmount())) < 0) return BigDecimal.ZERO;
        if (!isScopeMatched(couponUser.getScopeType(), couponUser.getScopeIds(), tour, tourPackage)) return BigDecimal.ZERO;
        BigDecimal discount;
        if ("RATE".equalsIgnoreCase(couponUser.getDiscountType())) {
            BigDecimal rate = couponUser.getDiscountRate() == null ? BigDecimal.ONE : couponUser.getDiscountRate();
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate));
            if (couponUser.getMaxDiscountAmount() != null && couponUser.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(couponUser.getMaxDiscountAmount());
            }
        } else {
            discount = zero(couponUser.getDiscountAmount());
        }
        BigDecimal capped = discount.min(orderAmount);
        if (capped.compareTo(BigDecimal.ZERO) < 0) {
            capped = BigDecimal.ZERO;
        }
        return capped.setScale(2, RoundingMode.HALF_UP);
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
        if (!"RATE".equalsIgnoreCase(coupon.getDiscountType())) {
            coupon.setDiscountType("AMOUNT");
            if (zero(coupon.getDiscountAmount()).compareTo(BigDecimal.ZERO) <= 0) throw new ServiceException("请输入优惠金额");
        } else if (coupon.getDiscountRate() == null || coupon.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 || coupon.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
            throw new ServiceException("折扣比例应大于0且小于1");
        }
        if (coupon.getValidStartTime() != null && coupon.getValidEndTime() != null && !coupon.getValidEndTime().isAfter(coupon.getValidStartTime())) {
            throw new ServiceException("请设置正确有效期");
        }
        coupon.setMinOrderAmount(zero(coupon.getMinOrderAmount()));
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

    private boolean isScopeMatched(String scopeType, String scopeIds, Tour tour, org.example.springboot.entity.TourPackage tourPackage) {
        if ("ALL_TOUR".equalsIgnoreCase(scopeType)) return true;
        Set<String> ids = Arrays.stream(StringUtils.defaultString(scopeIds).split("[,，、\\s]+"))
                .map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if ("TOUR".equalsIgnoreCase(scopeType)) return ids.contains(String.valueOf(tour.getId())) || ids.contains(tour.getCode());
        if ("TOUR_PACKAGE".equalsIgnoreCase(scopeType)) {
            if (tourPackage == null) return false;
            return ids.contains(tour.getId() + ":" + tourPackage.getId())
                    || ids.contains(tour.getCode() + ":" + tourPackage.getId())
                    || ids.contains(String.valueOf(tourPackage.getId()));
        }
        if ("TOUR_TYPE".equalsIgnoreCase(scopeType)) return ids.contains(tour.getTourType());
        return false;
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
                .set(CouponUser::getStatus, USER_COUPON_VOID));
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
