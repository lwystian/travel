package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.service.MiniappCustomerServiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "前台客服配置")
@RestController
@RequestMapping("/site/customer-service")
public class CustomerServiceConfigController {
    @Resource
    private MiniappCustomerServiceService miniappCustomerServiceService;

    @Operation(summary = "获取小程序统一客服的网页公开配置")
    @GetMapping("/public")
    public Result<Map<String, Object>> getPublicConfig() {
        return Result.success(miniappCustomerServiceService.getPublicConfig());
    }
}
