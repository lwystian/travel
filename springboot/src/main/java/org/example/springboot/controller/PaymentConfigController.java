package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.dto.AlipayConfigDTO;
import org.example.springboot.dto.PaymentConfigDTO;
import org.example.springboot.entity.PaymentConfig;
import org.example.springboot.service.PaymentConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付配置控制器
 */
@Tag(name = "支付配置接口")
@RestController
@RequestMapping("/payment-config")
public class PaymentConfigController {

    @Resource
    private PaymentConfigService paymentConfigService;

    /**
     * 获取已启用的支付方式列表
     */
    @Operation(summary = "获取已启用的支付方式列表")
    @GetMapping("/enabled")
    public Result<List<PaymentConfigDTO>> getEnabledPayments() {
        return Result.success(paymentConfigService.getEnabledPayments());
    }

    /**
     * 获取所有支付配置
     */
    @Operation(summary = "获取所有支付配置")
    @GetMapping("/list")
    public Result<List<PaymentConfigDTO>> getAllPayments() {
        return Result.success(paymentConfigService.getAllPayments());
    }

    /**
     * 根据ID获取配置
     */
    @Operation(summary = "获取支付配置详情")
    @GetMapping("/{id}")
    public Result<PaymentConfig> getById(@PathVariable Long id) {
        return Result.success(paymentConfigService.getById(id));
    }

    /**
     * 获取支付宝配置详情（根据paymentType获取）
     */
    @Operation(summary = "获取支付配置详情")
    @GetMapping("/detail")
    public Result<AlipayConfigDTO> getPaymentDetail(@RequestParam String paymentType) {
        return Result.success(paymentConfigService.getPaymentConfigDetail(paymentType));
    }

    /**
     * 保存支付宝配置
     */
    @Operation(summary = "保存支付宝配置")
    @PostMapping("/alipay/save")
    public Result<?> saveAlipayConfig(@RequestBody AlipayConfigDTO alipayConfig) {
        paymentConfigService.saveAlipayConfigDetail(alipayConfig);
        return Result.success("保存成功");
    }

    /**
     * 保存支付配置
     */
    @Operation(summary = "保存支付配置")
    @PostMapping("/save")
    public Result<?> savePayment(@RequestBody PaymentConfig config) {
        paymentConfigService.savePayment(config);
        return Result.success("保存成功");
    }

    /**
     * 更新支付配置
     */
    @Operation(summary = "更新支付配置")
    @PutMapping("/{id}")
    public Result<?> updatePayment(@PathVariable Long id, @RequestBody PaymentConfig config) {
        paymentConfigService.updatePayment(id, config);
        return Result.success("更新成功");
    }

    /**
     * 切换启用状态
     */
    @Operation(summary = "切换启用状态")
    @PutMapping("/{id}/toggle")
    public Result<?> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        paymentConfigService.toggleEnabled(id, enabled);
        return Result.success(enabled ? "已启用" : "已禁用");
    }

    /**
     * 删除配置
     */
    @Operation(summary = "删除支付配置")
    @DeleteMapping("/{id}")
    public Result<?> deletePayment(@PathVariable Long id) {
        paymentConfigService.deletePayment(id);
        return Result.success("删除成功");
    }
}
