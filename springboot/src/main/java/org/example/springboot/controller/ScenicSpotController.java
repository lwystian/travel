package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.ScenicSpotService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "景点管理接口")
@RestController
@RequestMapping("/scenic")
public class ScenicSpotController {
    @Resource
    private ScenicSpotService scenicSpotService;

    @Operation(summary = "分页查询景点")
    @GetMapping("/page")
    public Result<?> getScenicSpotsByPage(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false, defaultValue = "desc") String order) {
        Page<ScenicSpot> page = scenicSpotService.getScenicSpotsByPage(name, location, categoryId, currentPage, size, orderBy, order);
        return Result.success(page);
    }

    @Operation(summary = "获取景点详情")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        ScenicSpot spot = scenicSpotService.getById(id);
        return Result.success(spot);
    }

    @Operation(summary = "根据分类ID获取景点列表")
    @GetMapping("/category/{categoryId}")
    public Result<?> getScenicSpotsByCategoryId(@PathVariable Long categoryId) {
        List<ScenicSpot> spots = scenicSpotService.getScenicSpotsByCategoryId(categoryId);
        return Result.success(spots);
    }

    @Operation(summary = "新增景点")
    @PostMapping("/add")
    public Result<?> createScenicSpot(@RequestBody ScenicSpot spot) {
        SecurityGuards.requireAdmin();
        scenicSpotService.createScenicSpot(spot);
        // 返回新增的景点ID
        return Result.success(Map.of("id", spot.getId(), "message", "新增成功"));
    }

    @Operation(summary = "更新景点")
    @PutMapping("/{id}")
    public Result<?> updateScenicSpot(@PathVariable Long id, @RequestBody ScenicSpot spot) {
        SecurityGuards.requireAdmin();
        scenicSpotService.updateScenicSpot(id, spot);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除景点")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteScenicSpot(@PathVariable Long id) {
        SecurityGuards.requireAdmin();
        scenicSpotService.deleteScenicSpot(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取所有景点")
    @GetMapping("/all")
    public Result<?> getAll() {
        SecurityGuards.requireAdmin();
        List<ScenicSpot> list = scenicSpotService.getAll();
        return Result.success(list);
    }

    // 获取热门景点
    @Operation(summary = "获取热门景点")
    @GetMapping("/hot")
    public Result<?> getHotScenics(
            @RequestParam(required = false, defaultValue = "4") Integer limit) {
        List<ScenicSpot> hotScenics = scenicSpotService.getHotScenics(limit);
        return Result.success(hotScenics);
    }

    @Operation(summary = "获取搜索建议")
    @GetMapping("/suggestions")
    public Result<?> getSearchSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") Integer limit) {
        List<Map<String, Object>> suggestions = scenicSpotService.getSearchSuggestions(keyword, limit);
        return Result.success(suggestions);
    }

    @Operation(summary = "通过地址获取经纬度")
    @GetMapping("/geocode")
    public Result<?> geocodeAddress(@RequestParam String address) {
        try {
            return Result.success(Map.of(
                "address", address,
                "status", "success",
                "message", "请使用前端高德API进行地理编码"
            ));
        } catch (Exception e) {
            return Result.error("地理编码请求失败: " + e.getMessage());
        }
    }
} 
