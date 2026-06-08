package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.AdminPermissionUpdateDTO;
import org.example.springboot.dto.ContentModerationConfigDTO;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.AdminPermissionService;
import org.example.springboot.service.ContentModerationConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "后台权限与审核策略接口")
@RestController
@RequestMapping("/admin-governance")
public class AdminGovernanceController {
    @Resource
    private AdminPermissionService adminPermissionService;
    @Resource
    private ContentModerationConfigService contentModerationConfigService;

    @Operation(summary = "获取权限控制基础数据")
    @GetMapping("/permissions")
    public Result<?> getPermissions() {
        SecurityGuards.requireSuperAdmin();
        return Result.success(Map.of(
                "catalog", adminPermissionService.catalog(),
                "administrators", adminPermissionService.listAdministrators()
        ));
    }

    @Operation(summary = "保存管理员后台权限")
    @PutMapping("/permissions/{userId}")
    @OperationLog(operationType = "UPDATE", description = "配置管理员后台权限", targetType = "权限控制")
    public Result<?> savePermissions(@PathVariable Long userId, @RequestBody AdminPermissionUpdateDTO dto) {
        adminPermissionService.savePermissions(userId, dto == null ? null : dto.getPermissions(), SecurityGuards.requireSuperAdmin());
        return Result.success("权限配置已保存");
    }

    @Operation(summary = "获取内容审核策略")
    @GetMapping("/moderation-config")
    public Result<?> getModerationConfig() {
        SecurityGuards.requireSuperAdmin();
        return Result.success(contentModerationConfigService.getConfig());
    }

    @Operation(summary = "获取前台互动内容开放状态")
    @GetMapping("/public-interaction-config")
    public Result<?> getPublicInteractionConfig() {
        return Result.success(Map.of(
                "enabled", contentModerationConfigService.isPublicInteractionEnabled()
        ));
    }

    @Operation(summary = "保存内容审核策略")
    @PutMapping("/moderation-config")
    @OperationLog(operationType = "UPDATE", description = "配置管理员内容审核策略", targetType = "审核策略")
    public Result<?> saveModerationConfig(@RequestBody ContentModerationConfigDTO dto) {
        contentModerationConfigService.updateConfig(dto, SecurityGuards.requireSuperAdmin());
        return Result.success("审核策略已保存");
    }
}
