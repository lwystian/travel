package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.springboot.DTO.TourOrderCreateDTO;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.service.TourOrderService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "行程订单接口")
@RestController
@RequestMapping("/tour-order")
public class TourOrderController {

    @Resource
    private TourOrderService tourOrderService;

    @Operation(summary = "创建行程订单")
    @PostMapping
    public Result<?> createOrder(@Valid @RequestBody TourOrderCreateDTO dto) {
        TourOrder order = tourOrderService.createOrder(dto);
        return Result.success(order);
    }

    @Operation(summary = "支付订单")
    @PutMapping("/{id}/pay")
    public Result<?> payOrder(@PathVariable Long id, @RequestParam String paymentMethod) {
        tourOrderService.payOrder(id, paymentMethod);
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<?> cancelOrder(@PathVariable Long id) {
        tourOrderService.cancelOrder(id);
        return Result.success();
    }

    @Operation(summary = "退款订单（管理员）")
    @PutMapping("/{id}/refund")
    public Result<?> refundOrder(@PathVariable Long id) {
        tourOrderService.refundOrder(id);
        return Result.success();
    }

    @Operation(summary = "获取用户订单列表")
    @GetMapping("/user")
    public Result<?> getUserOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<TourOrder> page = tourOrderService.getUserOrders(status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<?> getOrderDetail(@PathVariable Long id) {
        TourOrder order = tourOrderService.getOrderDetail(id);
        return Result.success(order);
    }

    @Operation(summary = "管理员获取所有订单")
    @GetMapping("/all")
    public Result<?> getAllOrders(
            @RequestParam(defaultValue = "") String orderNo,
            @RequestParam(defaultValue = "") String contactName,
            @RequestParam(defaultValue = "") String contactPhone,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<TourOrder> page = tourOrderService.getAllOrders(orderNo, contactName, contactPhone, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "删除订单（管理员）")
    @DeleteMapping("/{id}")
    public Result<?> deleteOrder(@PathVariable Long id) {
        tourOrderService.deleteOrder(id);
        return Result.success();
    }

    @Operation(summary = "更新订单联系人信息")
    @PutMapping("/{id}/contact")
    public Result<?> updateContactInfo(
            @PathVariable Long id,
            @RequestParam String contactName,
            @RequestParam String contactPhone) {
        tourOrderService.updateContactInfo(id, contactName, contactPhone);
        return Result.success();
    }
}
