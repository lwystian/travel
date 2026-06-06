package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.PageContentConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "页面内容配置接口")
@RestController
@RequestMapping("/site/page-content")
public class PageContentConfigController {
    @Resource
    private PageContentConfigService pageContentConfigService;

    @Operation(summary = "获取公开页面内容")
    @GetMapping("/public/{pageKey}")
    public Result<Map<String, Object>> getPublicContent(@PathVariable String pageKey) {
        return Result.success(pageContentConfigService.getPublicContent(pageKey));
    }

    @Operation(summary = "获取后台页面内容")
    @GetMapping("/{pageKey}")
    public Result<Map<String, Object>> getAdminContent(@PathVariable String pageKey) {
        requireAdmin();
        return Result.success(pageContentConfigService.getAdminContent(pageKey));
    }

    @Operation(summary = "保存页面内容")
    @OperationLog(operationType = "UPDATE", description = "保存页面内容配置", targetType = "页面内容")
    @PostMapping("/{pageKey}")
    public Result<?> saveContent(@PathVariable String pageKey, @RequestBody Map<String, Object> content) {
        requireAdmin();
        pageContentConfigService.saveContent(pageKey, content);
        return Result.success("保存成功");
    }

    private void requireAdmin() {
        SecurityGuards.requirePermission("site-settings:manage");
    }
}
