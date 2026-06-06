package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TourPackage;
import org.example.springboot.entity.BatchPackage;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.TourPackageService;
import org.example.springboot.service.BatchPackageService;
import org.example.springboot.service.TourBatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "行程套餐和班期管理接口")
@RestController
@RequestMapping("/tour-detail")
public class TourDetailController {

    @Resource
    private TourPackageService tourPackageService;

    @Resource
    private BatchPackageService batchPackageService;

    @Resource
    private TourBatchService tourBatchService;

    // ==================== 行程套餐管理 ====================

    @Operation(summary = "获取行程套餐列表")
    @GetMapping("/packages/{tourId}")
    public Result<?> getTourPackages(@PathVariable Long tourId) {
        List<TourPackage> packages = tourPackageService.getByTourId(tourId);
        return Result.success(packages);
    }

    @Operation(summary = "新增行程套餐")
    @PostMapping("/packages")
    public Result<?> addTourPackage(@RequestBody TourPackage tourPackage) {
        SecurityGuards.requirePermission("tour:manage");
        tourPackageService.add(tourPackage);
        return Result.success();
    }

    @Operation(summary = "更新行程套餐")
    @PutMapping("/packages/{id}")
    public Result<?> updateTourPackage(@PathVariable Long id, @RequestBody TourPackage tourPackage) {
        SecurityGuards.requirePermission("tour:manage");
        tourPackage.setId(id);
        tourPackageService.update(tourPackage);
        return Result.success();
    }

    @Operation(summary = "删除行程套餐")
    @DeleteMapping("/packages/{id}")
    public Result<?> deleteTourPackage(@PathVariable Long id) {
        SecurityGuards.requirePermission("tour:manage");
        tourPackageService.delete(id);
        return Result.success();
    }

    // ==================== 批次套餐管理 ====================

    @Operation(summary = "获取批次套餐列表")
    @GetMapping("/batch-packages/{tourId}")
    public Result<?> getBatchPackages(@PathVariable Long tourId) {
        List<BatchPackage> packages = batchPackageService.getByTourId(tourId);
        return Result.success(packages);
    }

    @Operation(summary = "新增批次套餐")
    @PostMapping("/batch-packages")
    public Result<?> addBatchPackage(@RequestBody BatchPackage batchPackage) {
        SecurityGuards.requirePermission("tour:manage");
        batchPackageService.add(batchPackage);
        return Result.success();
    }

    @Operation(summary = "更新批次套餐")
    @PutMapping("/batch-packages/{id}")
    public Result<?> updateBatchPackage(@PathVariable Long id, @RequestBody BatchPackage batchPackage) {
        SecurityGuards.requirePermission("tour:manage");
        batchPackage.setId(id);
        batchPackageService.update(batchPackage);
        return Result.success();
    }

    @Operation(summary = "删除批次套餐")
    @DeleteMapping("/batch-packages/{id}")
    public Result<?> deleteBatchPackage(@PathVariable Long id) {
        SecurityGuards.requirePermission("tour:manage");
        batchPackageService.delete(id);
        return Result.success();
    }

    // ==================== 出发班期管理 ====================

    @Operation(summary = "获取出发班期列表")
    @GetMapping("/batches/{tourId}")
    public Result<?> getTourBatches(@PathVariable Long tourId) {
        List<TourBatch> batches = tourBatchService.getByTourId(tourId);
        return Result.success(batches);
    }

    @Operation(summary = "新增出发班期")
    @PostMapping("/batches")
    public Result<?> addTourBatch(@RequestBody TourBatch tourBatch) {
        SecurityGuards.requirePermission("tour:manage");
        tourBatchService.add(tourBatch);
        return Result.success();
    }

    @Operation(summary = "批量新增出发班期")
    @PostMapping("/batches/batch")
    public Result<?> addTourBatchesBatch(@RequestBody List<TourBatch> tourBatches) {
        SecurityGuards.requirePermission("tour:manage");
        tourBatchService.addBatch(tourBatches);
        return Result.success();
    }

    @Operation(summary = "更新出发班期")
    @PutMapping("/batches/{id}")
    public Result<?> updateTourBatch(@PathVariable Long id, @RequestBody TourBatch tourBatch) {
        SecurityGuards.requirePermission("tour:manage");
        tourBatch.setId(id);
        tourBatchService.update(tourBatch);
        return Result.success();
    }

    @Operation(summary = "删除出发班期")
    @DeleteMapping("/batches/{id}")
    public Result<?> deleteTourBatch(@PathVariable Long id) {
        SecurityGuards.requirePermission("tour:manage");
        tourBatchService.delete(id);
        return Result.success();
    }
}
