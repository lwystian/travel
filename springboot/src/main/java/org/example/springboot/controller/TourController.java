package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.dto.TourDetailDTO;
import org.example.springboot.dto.HomeRecommendDTO;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Tour;
import org.example.springboot.service.TourService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "行程管理接口")
@RestController
@RequestMapping("/tour")
public class TourController {
    private static final Logger logger = LoggerFactory.getLogger(TourController.class);

    @Resource
    private TourService tourService;

    @Operation(summary = "分页查询行程")
    @GetMapping("/page")
    public Result<?> getToursByPage(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String tourType,
            @RequestParam(defaultValue = "") String city,
            @RequestParam(defaultValue = "") String destination,
            @RequestParam(defaultValue = "") String days,
            @RequestParam(defaultValue = "") String month,
            @RequestParam(defaultValue = "") String priceRange,
            @RequestParam(defaultValue = "") String theme,
            @RequestParam(defaultValue = "default") String sortType,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Tour> page = tourService.getToursByPage(
            keyword, tourType, city, destination, days, month, priceRange, theme, sortType, currentPage, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取前台行程筛选项")
    @GetMapping("/filters")
    public Result<?> getTourFilters(@RequestParam(defaultValue = "") String keyword) {
        return Result.success(tourService.getTourFilters(keyword));
    }

    @Operation(summary = "获取前台热门行程关键词")
    @GetMapping("/hot-keywords")
    public Result<?> getHotKeywords() {
        return Result.success(tourService.getHotKeywords(8));
    }

    @Operation(summary = "获取行程预订页精选推荐")
    @GetMapping("/ticket-featured")
    public Result<?> getTicketFeaturedTours() {
        return Result.success(tourService.getTicketFeaturedTours(4));
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

    // ==================== 首页推荐管理接口 ====================

    @Operation(summary = "获取精选行程（首页使用）")
    @GetMapping("/featured")
    public Result<?> getFeaturedTours() {
        List<Tour> tours = tourService.getFeaturedTours();
        return Result.success(tours);
    }

    @Operation(summary = "获取更多推荐行程（首页使用）")
    @GetMapping("/more")
    public Result<?> getMoreTours() {
        List<Tour> tours = tourService.getMoreTours();
        return Result.success(tours);
    }

    @Operation(summary = "获取首页推荐列表（后台管理使用）")
    @GetMapping("/recommends")
    public Result<?> getRecommends(@RequestParam(required = false) String type) {
        List<HomeRecommendDTO> recommends;
        if (type != null && !type.isEmpty()) {
            recommends = tourService.getRecommendsByTypeDTO(type);
        } else {
            recommends = tourService.getAllRecommendsDTO();
        }
        return Result.success(recommends);
    }

    @Operation(summary = "批量保存首页推荐")
    @PostMapping("/recommends")
    public Result<?> saveRecommends(@RequestBody java.util.Map<String, Object> body) {
        String type = (String) body.get("type");
        @SuppressWarnings("unchecked")
        java.util.List<Number> tourIdList = (java.util.List<Number>) body.get("tourIds");

        if (type == null || type.isEmpty()) {
            return Result.error("类型不能为空");
        }

        if (tourIdList == null || tourIdList.isEmpty()) {
            tourService.clearRecommendsByType(type);
            return Result.success();
        }

        List<Long> tourIds = new java.util.ArrayList<>();
        for (Number id : tourIdList) {
            tourIds.add(id.longValue());
        }

        tourService.saveRecommends(type, tourIds);
        return Result.success();
    }

    @Operation(summary = "清空指定类型的首页推荐")
    @DeleteMapping("/recommends/clear")
    public Result<?> clearRecommends(@RequestParam String type) {
        tourService.clearRecommendsByType(type);
        return Result.success();
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
        logger.debug("Update tour images request: id={}, imageCount={}", id, images == null ? 0 : images.size());
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

    @Operation(summary = "根据景点推荐相关行程")
    @GetMapping("/recommended")
    public Result<?> getRecommendedTours(
            @RequestParam(required = false) String scenicName,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "6") Integer limit) {
        List<Tour> tours = tourService.getRecommendedToursByScenic(scenicName, location, limit);
        return Result.success(tours);
    }

    @Operation(summary = "删除首页推荐")
    @DeleteMapping("/recommend/{id}")
    public Result<?> deleteRecommend(@PathVariable Long id) {
        tourService.deleteRecommend(id);
        return Result.success();
    }

    @Operation(summary = "更新推荐排序")
    @PutMapping("/recommend/sort")
    public Result<?> updateRecommendSort(@RequestBody java.util.Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        java.util.List<Number> ids = (java.util.List<Number>) body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("ID列表不能为空");
        }
        tourService.updateRecommendSort(ids);
        return Result.success();
    }
}
