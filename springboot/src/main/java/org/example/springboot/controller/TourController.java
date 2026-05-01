package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.DTO.TourDetailDTO;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Tour;
import org.example.springboot.service.TourService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "行程管理接口")
@RestController
@RequestMapping("/tour")
public class TourController {

    @Resource
    private TourService tourService;

    @Operation(summary = "分页查询行程")
    @GetMapping("/page")
    public Result<?> getToursByPage(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String tourType,
            @RequestParam(defaultValue = "") String city,
            @RequestParam(defaultValue = "") String destination,
            @RequestParam(defaultValue = "") String theme,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Tour> page = tourService.getToursByPage(title, tourType, city, destination, theme, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "获取所有行程")
    @GetMapping("/all")
    public Result<?> getAllTours() {
        List<Tour> tours = tourService.getAllTours();
        return Result.success(tours);
    }

    @Operation(summary = "获取所有上架行程（前台使用）")
    @GetMapping("/list")
    public Result<?> getActiveTours() {
        List<Tour> tours = tourService.getActiveTours();
        return Result.success(tours);
    }

    @Operation(summary = "根据ID获取行程详情（简单信息）")
    @GetMapping("/{id}")
    public Result<?> getTourById(@PathVariable Long id) {
        Tour tour = tourService.getTourById(id);
        return Result.success(tour);
    }

    @Operation(summary = "根据ID获取行程完整详情（包含套餐、批次等）")
    @GetMapping("/{id}/detail")
    public Result<?> getTourDetail(@PathVariable Long id) {
        TourDetailDTO detail = tourService.getTourDetail(id);
        return Result.success(detail);
    }

    @Operation(summary = "新增行程")
    @PostMapping
    public Result<?> addTour(@RequestBody Tour tour) {
        tourService.addTour(tour);
        return Result.success();
    }

    @Operation(summary = "更新行程信息")
    @PutMapping("/{id}")
    public Result<?> updateTour(@PathVariable Long id, @RequestBody Tour tour) {
        tour.setId(id);
        tourService.updateTour(tour);
        return Result.success();
    }

    @Operation(summary = "删除行程")
    @DeleteMapping("/{id}")
    public Result<?> deleteTour(@PathVariable Long id) {
        tourService.deleteTour(id);
        return Result.success();
    }

    @Operation(summary = "更新行程状态")
    @PutMapping("/{id}/status")
    public Result<?> updateTourStatus(@PathVariable Long id, @RequestParam Integer status) {
        tourService.updateTourStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "更新行程图片")
    @PutMapping("/{id}/images")
    public Result<?> updateTourImages(@PathVariable Long id, @RequestBody java.util.Map<String, List<String>> body) {
        List<String> images = body.get("images");
        System.out.println("=== 更新图片 === id=" + id + " images=" + images);
        tourService.updateTourImages(id, images);
        return Result.success();
    }

    @Operation(summary = "更新行程视频")
    @PutMapping("/{id}/video")
    public Result<?> updateTourVideo(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        String videoUrl = (String) body.get("videoUrl");
        String videoPoster = (String) body.get("videoPoster");
        Integer videoEnabled = body.get("videoEnabled") != null ? ((Number) body.get("videoEnabled")).intValue() : 0;
        tourService.updateTourVideo(id, videoUrl, videoPoster, videoEnabled);
        return Result.success();
    }
}
