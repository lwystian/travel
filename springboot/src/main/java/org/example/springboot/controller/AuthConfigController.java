package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.dto.AuthConfigTestRequestDTO;
import org.example.springboot.dto.AuthConfigTestResultDTO;
import org.example.springboot.dto.EmailSmtpConfigDTO;
import org.example.springboot.dto.GeetestConfigDTO;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.AuthConfigService;
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

    @Operation(summary = "测试阿里云短信配置")
    @OperationLog(operationType = "CHECK", description = "测试阿里云短信配置", targetType = "认证配置")
    @PostMapping("/sms/test")
    public Result<AuthConfigTestResultDTO> testSmsConfig(@RequestBody AuthConfigTestRequestDTO dto) {
        requireAdmin();
        return Result.success(authConfigService.testSmsConfig(
                dto == null ? null : dto.getPhone(),
                dto == null ? null : dto.getTemplateCode()
        ));
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

    @Operation(summary = "测试极验验证配置")
    @OperationLog(operationType = "CHECK", description = "测试极验验证配置", targetType = "认证配置")
    @PostMapping("/geetest/test")
    public Result<AuthConfigTestResultDTO> testGeetestConfig() {
        requireAdmin();
        return Result.success(authConfigService.testGeetestConfig());
    }

    @Operation(summary = "获取SMTP邮件配置")
    @GetMapping("/email")
    public Result<EmailSmtpConfigDTO> getEmailConfig() {
        requireAdmin();
        return Result.success(authConfigService.getEmailConfigForAdmin());
    }

    @Operation(summary = "保存SMTP邮件配置")
    @OperationLog(operationType = "UPDATE", description = "保存SMTP邮件配置", targetType = "认证配置")
    @PostMapping("/email")
    public Result<?> saveEmailConfig(@RequestBody EmailSmtpConfigDTO dto) {
        requireAdmin();
        authConfigService.saveEmailConfig(dto);
        return Result.success("保存成功");
    }

    @Operation(summary = "测试SMTP邮件配置")
    @OperationLog(operationType = "CHECK", description = "测试SMTP邮件配置", targetType = "认证配置")
    @PostMapping("/email/test")
    public Result<AuthConfigTestResultDTO> testEmailConfig(@RequestBody AuthConfigTestRequestDTO dto) {
        requireAdmin();
        return Result.success(authConfigService.testEmailConfig(dto == null ? null : dto.getEmail()));
    }

    private void requireAdmin() {
        SecurityGuards.requirePermission("auth-config:manage");
    }
}
