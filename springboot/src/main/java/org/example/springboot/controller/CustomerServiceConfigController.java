package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.CustomerServiceConfigDTO;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.CustomerServiceConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台客服配置")
@RestController
@RequestMapping("/site/customer-service")
public class CustomerServiceConfigController {
    @Resource
    private CustomerServiceConfigService customerServiceConfigService;

    @Operation(summary = "获取官网在线客服公开配置")
    @GetMapping("/public")
    public Result<CustomerServiceConfigDTO> getPublicConfig() {
        return Result.success(customerServiceConfigService.getPublicConfig());
    }

    @Operation(summary = "获取官网在线客服后台配置")
    @GetMapping
    public Result<CustomerServiceConfigDTO> getAdminConfig() {
        SecurityGuards.requirePermission("site-settings:manage");
        return Result.success(customerServiceConfigService.getAdminConfig());
    }

    @Operation(summary = "保存官网在线客服配置")
    @OperationLog(operationType = "UPDATE", description = "保存官网在线客服配置", targetType = "网站设置")
    @PostMapping
    public Result<?> saveConfig(@RequestBody CustomerServiceConfigDTO dto) {
        SecurityGuards.requirePermission("site-settings:manage");
        customerServiceConfigService.saveConfig(dto);
        return Result.success("保存成功");
    }
}
