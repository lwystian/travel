package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.SiteAssetConfigDTO;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.security.RolePermission;
import org.example.springboot.service.SiteAssetConfigService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "site asset config")
@RestController
@RequestMapping("/site/assets")
public class SiteAssetConfigController {
    @Resource
    private SiteAssetConfigService siteAssetConfigService;

    @Operation(summary = "get public site asset config")
    @GetMapping("/public")
    public Result<SiteAssetConfigDTO> getPublicConfig() {
        return Result.success(siteAssetConfigService.getPublicConfig());
    }

    @Operation(summary = "get admin site asset config")
    @GetMapping
    public Result<SiteAssetConfigDTO> getAdminConfig() {
        requireAdmin();
        return Result.success(siteAssetConfigService.getAdminConfig());
    }

    @Operation(summary = "save site asset config")
    @OperationLog(operationType = "UPDATE", description = "save site asset config", targetType = "site assets")
    @PostMapping
    public Result<?> saveConfig(@RequestBody SiteAssetConfigDTO dto) {
        requireAdmin();
        siteAssetConfigService.saveConfig(dto);
        return Result.success("saved");
    }

    private void requireAdmin() {
        var user = JwtTokenUtils.getCurrentUser();
        if (!RolePermission.isAdmin(user)) {
            throw new ServiceException("no permission");
        }
    }
}
