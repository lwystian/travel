package org.example.springboot.controller;

import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.dto.TourProductSourceConfigDTO;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.MiniappTourAdapterService;
import org.example.springboot.service.TourProductSourceConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/site/tour-source")
public class TourProductSourceConfigController {
    @Resource
    private TourProductSourceConfigService configService;

    @Resource
    private MiniappTourAdapterService miniappTourAdapterService;

    @GetMapping("/public")
    public Result<?> publicConfig() {
        return Result.success(configService.publicView());
    }

    @GetMapping
    public Result<?> getConfig() {
        SecurityGuards.requirePermission("site-settings:manage");
        return Result.success(configService.getConfig());
    }

    @PostMapping
    @OperationLog(operationType = "UPDATE", description = "更新官网行程商品来源", targetType = "网站设置")
    public Result<?> saveConfig(@RequestBody TourProductSourceConfigDTO dto) {
        SecurityGuards.requirePermission("site-settings:manage");
        configService.saveConfig(dto);
        miniappTourAdapterService.invalidateSummaryCache();
        return Result.success(configService.getConfig());
    }

    @PostMapping("/check")
    public Result<?> checkConnection(@RequestBody(required = false) TourProductSourceConfigDTO dto) {
        SecurityGuards.requirePermission("site-settings:manage");
        return Result.success(miniappTourAdapterService.checkConnection(dto));
    }
}
