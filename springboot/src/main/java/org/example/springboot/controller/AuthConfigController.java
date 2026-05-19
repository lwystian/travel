package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.dto.GeetestConfigDTO;
import org.example.springboot.service.AuthConfigService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证配置接口")
@RestController
@RequestMapping("/auth/config")
public class AuthConfigController {
    @Resource
    private AuthConfigService authConfigService;

    @Operation(summary = "获取阿里云短信配置")
    @GetMapping("/sms")
    public Result<AliyunSmsConfigDTO> getSmsConfig() {
        requireAdmin();
        return Result.success(authConfigService.getSmsConfigForAdmin());
    }

    @Operation(summary = "保存阿里云短信配置")
    @OperationLog(operationType = "UPDATE", description = "保存阿里云短信配置", targetType = "认证配置")
    @PostMapping("/sms")
    public Result<?> saveSmsConfig(@RequestBody AliyunSmsConfigDTO dto) {
        requireAdmin();
        authConfigService.saveSmsConfig(dto);
        return Result.success("保存成功");
    }

    @Operation(summary = "获取极验配置")
    @GetMapping("/geetest")
    public Result<GeetestConfigDTO> getGeetestConfig() {
        requireAdmin();
        return Result.success(authConfigService.getGeetestConfigForAdmin());
    }

    @Operation(summary = "保存极验配置")
    @OperationLog(operationType = "UPDATE", description = "保存极验验证配置", targetType = "认证配置")
    @PostMapping("/geetest")
    public Result<?> saveGeetestConfig(@RequestBody GeetestConfigDTO dto) {
        requireAdmin();
        authConfigService.saveGeetestConfig(dto);
        return Result.success("保存成功");
    }

    private void requireAdmin() {
        var user = JwtTokenUtils.getCurrentUser();
        if (user == null || !"ADMIN".equals(user.getRoleCode())) {
            throw new org.example.springboot.exception.ServiceException("无权限");
        }
    }
}
