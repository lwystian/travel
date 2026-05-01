package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.FrequentTraveler;
import org.example.springboot.service.FrequentTravelerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常用出行人控制器
 */
@Tag(name = "常用出行人接口")
@RestController
@RequestMapping("/frequent-traveler")
public class FrequentTravelerController {

    @Resource
    private FrequentTravelerService frequentTravelerService;

    @Operation(summary = "获取当前用户的常用出行人列表")
    @GetMapping
    public Result<List<FrequentTraveler>> getMyTravelers(@RequestAttribute Long userId) {
        return Result.success(frequentTravelerService.getByUserId(userId));
    }

    @Operation(summary = "保存常用出行人")
    @PostMapping
    public Result<FrequentTraveler> saveTraveler(
            @RequestAttribute Long userId,
            @RequestBody FrequentTraveler traveler) {
        FrequentTraveler result = frequentTravelerService.saveOrUpdate(userId, traveler);
        if (result == null) {
            return Result.error("无权修改此出行人信息");
        }
        return Result.success(result);
    }

    @Operation(summary = "更新常用出行人")
    @PutMapping("/{id}")
    public Result<FrequentTraveler> updateTraveler(
            @RequestAttribute Long userId,
            @PathVariable Long id,
            @RequestBody FrequentTraveler traveler) {
        traveler.setId(id);
        FrequentTraveler result = frequentTravelerService.saveOrUpdate(userId, traveler);
        if (result == null) {
            return Result.error("无权修改此出行人信息");
        }
        return Result.success(result);
    }

    @Operation(summary = "删除常用出行人")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTraveler(
            @RequestAttribute Long userId,
            @PathVariable Long id) {
        boolean success = frequentTravelerService.delete(userId, id);
        if (!success) {
            return Result.error("删除失败或无权删除此出行人");
        }
        return Result.success();
    }

    @Operation(summary = "设置默认出行人")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(
            @RequestAttribute Long userId,
            @PathVariable Long id) {
        frequentTravelerService.setDefault(userId, id);
        return Result.success();
    }
}
