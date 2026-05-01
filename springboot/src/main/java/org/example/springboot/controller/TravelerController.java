package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Traveler;
import org.example.springboot.service.TravelerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 出行人信息控制器
 */
@Tag(name = "出行人信息接口")
@RestController
@RequestMapping("/traveler")
public class TravelerController {

    @Resource
    private TravelerService travelerService;

    @Operation(summary = "根据订单ID获取出行人列表")
    @GetMapping("/order/{orderId}")
    public Result<List<Traveler>> getByOrderId(@PathVariable Long orderId) {
        return Result.success(travelerService.getByOrderId(orderId));
    }

    @Operation(summary = "根据订单号获取出行人列表")
    @GetMapping("/order-no/{orderNo}")
    public Result<List<Traveler>> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(travelerService.getByOrderNo(orderNo));
    }

    @Operation(summary = "保存出行人信息")
    @PostMapping("/save/{orderId}")
    public Result<Void> saveTravelers(
            @PathVariable Long orderId,
            @RequestParam String orderNo,
            @RequestBody List<Traveler> travelers) {
        travelerService.saveBatch(orderId, orderNo, travelers);
        return Result.success();
    }
}
