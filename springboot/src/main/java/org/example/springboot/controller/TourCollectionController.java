package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TourCollection;
import org.example.springboot.service.TourCollectionService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "行程收藏接口")
@RestController
@RequestMapping("/tour-collection")
public class TourCollectionController {
    @Resource
    private TourCollectionService tourCollectionService;

    @Operation(summary = "添加行程收藏")
    @PostMapping("/{tourId}")
    public Result<?> addCollection(@PathVariable Long tourId) {
        tourCollectionService.addCollection(tourId);
        return Result.success("收藏成功");
    }

    @Operation(summary = "取消行程收藏")
    @DeleteMapping("/{tourId}")
    public Result<?> cancelCollection(@PathVariable Long tourId) {
        tourCollectionService.cancelCollection(tourId);
        return Result.success("取消收藏成功");
    }

    @Operation(summary = "查询当前用户是否已收藏某行程")
    @GetMapping("/is-collected/{tourId}")
    public Result<?> isCollected(@PathVariable Long tourId) {
        return Result.success(tourCollectionService.isCollected(tourId));
    }

    @Operation(summary = "查询用户收藏的行程列表")
    @GetMapping("/user")
    public Result<?> getUserCollections(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Long queryUserId = userId == null ? JwtTokenUtils.getCurrentUser().getId() : userId;
        Page<TourCollection> page = tourCollectionService.getUserCollections(queryUserId, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "查询用户收藏的行程ID列表")
    @GetMapping("/user/ids")
    public Result<?> getUserCollectionIds(@RequestParam(required = false) Long userId) {
        Long queryUserId = userId == null ? JwtTokenUtils.getCurrentUser().getId() : userId;
        List<Long> ids = tourCollectionService.getUserCollectionIds(queryUserId);
        return Result.success(ids);
    }
}
