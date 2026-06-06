package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.SiteAccessConfigDTO;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.SiteAccessConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "网站访问控制配置")
@RestController
@RequestMapping("/site/access")
public class SiteAccessConfigController {
    @Resource
    private SiteAccessConfigService siteAccessConfigService;

    @Operation(summary = "获取网站访问公开配置")
    @GetMapping("/public")
    public Result<SiteAccessConfigDTO> getPublicConfig() {
        return Result.success(siteAccessConfigService.getPublicConfig());
    }

    @Operation(summary = "获取网站访问后台配置")
    @GetMapping
    public Result<SiteAccessConfigDTO> getAdminConfig() {
        requireAdmin();
        return Result.success(siteAccessConfigService.getAdminConfig());
    }

    @Operation(summary = "保存网站访问控制配置")
    @OperationLog(operationType = "UPDATE", description = "保存网站访问控制配置", targetType = "网站设置")
    @PostMapping
    public Result<?> saveConfig(@RequestBody SiteAccessConfigDTO dto) {
        requireAdmin();
        siteAccessConfigService.saveConfig(dto);
        return Result.success("保存成功");
    }

    private void requireAdmin() {
        SecurityGuards.requirePermission("site-settings:manage");
    }
}
