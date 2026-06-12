package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Coupon;
import org.example.springboot.entity.CouponUser;
import org.example.springboot.entity.Tour;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.entity.TourPackage;
import org.example.springboot.entity.Traveler;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.TourMapper;
import org.example.springboot.mapper.TourBatchMapper;
import org.example.springboot.mapper.TourPackageMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.CouponService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/coupon")
@Tag(name = "优惠券接口")
public class CouponController {
    @Resource
    private CouponService couponService;
    @Resource
    private TourMapper tourMapper;
    @Resource
    private TourBatchMapper tourBatchMapper;
    @Resource
    private TourPackageMapper tourPackageMapper;
    @Resource
    private UserMapper userMapper;

    @GetMapping("/admin/page")
    @Operation(summary = "后台分页查询优惠券")
    public Result<Page<Coupon>> page(@RequestParam(defaultValue = "1") Integer currentPage,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(defaultValue = "") String keyword,
                                     @RequestParam(required = false) Integer status) {
        SecurityGuards.requirePermission("coupon:manage");
        return Result.success(couponService.pageCoupons(currentPage, pageSize, keyword, status));
    }

    @PostMapping("/admin")
    @Operation(summary = "新增/编辑优惠券")
    @OperationLog(operationType = "UPDATE", description = "保存优惠券", targetType = "优惠券")
    public Result<?> save(@RequestBody Coupon coupon) {
        SecurityGuards.requirePermission("coupon:manage");
        couponService.saveCoupon(coupon);
        return Result.success();
    }

    @PutMapping("/admin/{id}/status")
    @Operation(summary = "上下架优惠券")
    @OperationLog(operationType = "UPDATE_STATUS", description = "调整优惠券启用状态", targetType = "优惠券")
    public Result<?> status(@PathVariable Long id, @RequestParam Integer status) {
        SecurityGuards.requirePermission("coupon:manage");
        couponService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "删除优惠券")
    @OperationLog(operationType = "DELETE", description = "删除优惠券", targetType = "优惠券")
    public Result<?> delete(@PathVariable Long id) {
        SecurityGuards.requirePermission("coupon:manage");
        couponService.deleteCoupon(id);
        return Result.success();
    }

    @GetMapping("/admin/scope-options")
    @Operation(summary = "优惠券适用范围候选项")
    public Result<?> scopeOptions() {
        SecurityGuards.requirePermission("coupon:manage");
        List<Tour> tours = tourMapper.selectList(null);
        List<TourPackage> packages = tourPackageMapper.selectList(null);
        List<TourBatch> batches = tourBatchMapper.selectList(null);
        Map<Long, List<Map<String, Object>>> packageMap = packages.stream().collect(Collectors.groupingBy(
                TourPackage::getTourId,
                Collectors.mapping(pkg -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", pkg.getId());
                    item.put("name", pkg.getName() == null ? "" : pkg.getName());
                    item.put("status", pkg.getStatus() == null ? 1 : pkg.getStatus());
                    return item;
                }, Collectors.toList())
        ));
        Map<Long, List<Map<String, Object>>> batchMap = batches.stream().collect(Collectors.groupingBy(
                TourBatch::getTourId,
                Collectors.mapping(batch -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", batch.getId());
                    item.put("departureDate", batch.getDepartureDate() == null ? "" : batch.getDepartureDate().toString());
                    item.put("status", batch.getStatus() == null ? "" : batch.getStatus());
                    item.put("remaining", batch.getRemaining() == null ? 0 : batch.getRemaining());
                    item.put("occupied", batch.getOccupied() == null ? 0 : batch.getOccupied());
                    item.put("packageIds", batch.getPackageIds() == null ? "" : batch.getPackageIds());
                    return item;
                }, Collectors.toList())
        ));
        List<Map<String, Object>> records = tours.stream().map(tour -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tour.getId());
            item.put("code", tour.getCode() == null ? "" : tour.getCode());
            item.put("title", tour.getTitle() == null ? "" : tour.getTitle());
            item.put("status", tour.getStatus() == null ? 1 : tour.getStatus());
            item.put("packages", packageMap.getOrDefault(tour.getId(), List.of()));
            item.put("batches", batchMap.getOrDefault(tour.getId(), List.of()));
            return item;
        }).toList();
        return Result.success(records);
    }

    @GetMapping("/admin/users")
    @Operation(summary = "优惠券发放用户候选项")
    public Result<?> enabledUsers(@RequestParam(defaultValue = "") String keyword,
                                  @RequestParam(defaultValue = "1") Integer currentPage,
                                  @RequestParam(defaultValue = "20") Integer pageSize) {
        SecurityGuards.requirePermission("coupon:manage");
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1)
                .and(org.apache.commons.lang3.StringUtils.isNotBlank(keyword), w -> w
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByDesc(User::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page = userMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(currentPage, pageSize), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername() == null ? "" : user.getUsername());
            item.put("nickname", user.getNickname() == null ? "" : user.getNickname());
            item.put("phone", user.getPhone() == null ? "" : user.getPhone());
            return item;
        }).toList();
        return Result.success(Map.of("records", records, "total", page.getTotal()));
    }

    @PostMapping("/admin/{id}/issue")
    @Operation(summary = "发放优惠券")
    @OperationLog(operationType = "CREATE", description = "发放优惠券", targetType = "优惠券")
    public Result<?> issue(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityGuards.requirePermission("coupon:manage");
        boolean allUsers = Boolean.TRUE.equals(body.get("allUsers"));
        int count;
        if (allUsers) {
            count = couponService.issueToAllUsers(id);
        } else {
            @SuppressWarnings("unchecked")
            List<Object> rawIds = (List<Object>) body.get("userIds");
            List<Long> userIds = rawIds == null ? List.of() : rawIds.stream().map(value -> Long.valueOf(String.valueOf(value))).toList();
            count = couponService.issueToUsers(id, userIds);
        }
        return Result.success(Map.of("issued", count));
    }

    @GetMapping("/receivable")
    @Operation(summary = "前台可领取优惠券")
    public Result<List<Coupon>> receivable() {
        return Result.success(couponService.listReceivable());
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "领取优惠券")
    @OperationLog(operationType = "CREATE", description = "领取优惠券", targetType = "优惠券")
    public Result<CouponUser> receive(@PathVariable Long id) {
        return Result.success(couponService.receive(id));
    }

    @GetMapping("/my")
    @Operation(summary = "我的优惠券")
    public Result<List<CouponUser>> my(@RequestParam(required = false) Integer status) {
        return Result.success(couponService.myCoupons(status));
    }

    @GetMapping("/available")
    @Operation(summary = "当前订单可用优惠券")
    public Result<List<CouponUser>> available(@RequestParam Long tourId,
                                              @RequestParam(required = false) Long packageId,
                                              @RequestParam BigDecimal orderAmount) {
        return Result.success(couponService.availableForOrder(tourId, packageId, orderAmount));
    }

    @PostMapping("/order/{orderId}/available")
    @Operation(summary = "订单确认页可用优惠券")
    public Result<List<CouponUser>> availableForOrder(@PathVariable Long orderId,
                                                      @RequestBody(required = false) List<Traveler> travelers) {
        return Result.success(couponService.availableForOrder(orderId, travelers));
    }

    @PutMapping("/order/{orderId}/apply")
    @Operation(summary = "订单确认页应用优惠券")
    public Result<TourOrder> applyForOrder(@PathVariable Long orderId,
                                           @RequestParam(required = false) Long couponUserId) {
        return Result.success(couponService.applyCouponToOrder(orderId, couponUserId));
    }
}
