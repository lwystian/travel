package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.SiteFooterConfigDTO;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.SiteFooterConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "网站页脚配置接口")
@RestController
@RequestMapping("/site/footer")
public class SiteFooterConfigController {
    @Resource
    private SiteFooterConfigService siteFooterConfigService;

    @Operation(summary = "获取网站页脚公开配置")
    @GetMapping("/public")
    public Result<SiteFooterConfigDTO> getPublicConfig() {
        return Result.success(siteFooterConfigService.getPublicConfig());
    }

    @Operation(summary = "获取网站页脚后台配置")
    @GetMapping
    public Result<SiteFooterConfigDTO> getAdminConfig() {
        requireAdmin();
        return Result.success(siteFooterConfigService.getAdminConfig());
    }

    @Operation(summary = "保存网站页脚配置")
    @OperationLog(operationType = "UPDATE", description = "保存网站页脚配置", targetType = "网站页脚")
    @PostMapping
    public Result<?> saveConfig(@RequestBody SiteFooterConfigDTO dto) {
        requireAdmin();
        siteFooterConfigService.saveConfig(dto);
        return Result.success("保存成功");
    }

    private void requireAdmin() {
        SecurityGuards.requirePermission("site-footer:manage");
    }
}
