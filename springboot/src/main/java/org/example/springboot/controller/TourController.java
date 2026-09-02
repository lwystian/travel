package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.dto.TourDetailDTO;
import org.example.springboot.dto.HomeRecommendDTO;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Tour;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.MiniappTourAdapterService;
import org.example.springboot.service.TourProductSourceConfigService;
import org.example.springboot.service.TourSourceDisplayConfigService;
import org.example.springboot.service.TourService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Tag(name = "行程管理接口")
@RestController
@RequestMapping("/tour")
public class TourController {
    private static final Logger logger = LoggerFactory.getLogger(TourController.class);

    @Resource
    private TourService tourService;

    @Resource
    private MiniappTourAdapterService miniappTourAdapterService;

    @Resource
    private TourProductSourceConfigService tourProductSourceConfigService;

    @Resource
    private TourSourceDisplayConfigService tourSourceDisplayConfigService;

    @Operation(summary = "分页查询行程")
    @GetMapping("/page")
    public Result<?> getToursByPage(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String tourType,
            @RequestParam(defaultValue = "") String city,
            @RequestParam(defaultValue = "") String destination,
            @RequestParam(defaultValue = "") String days,
            @RequestParam(defaultValue = "") String month,
            @RequestParam(defaultValue = "") String priceRange,
            @RequestParam(defaultValue = "") String searchMode,
            @RequestParam(defaultValue = "") String intentDestination,
            @RequestParam(defaultValue = "") String matchMode,
            @RequestParam(defaultValue = "default") String sortType,
            @RequestParam(defaultValue = "false") Boolean includeInactive,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        String effectiveKeyword = !keyword.isBlank() ? keyword : search;
        boolean canIncludeInactive = Boolean.TRUE.equals(includeInactive);
        if (canIncludeInactive) {
            SecurityGuards.requirePermission("tour:manage");
        }
        if (canIncludeInactive && useMiniappSource()) {
            return Result.success(miniappTourAdapterService.getManageTourPage(
                    effectiveKeyword, tourType, city, destination, currentPage, pageSize));
        }
        if (!canIncludeInactive && useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    () -> miniappTourAdapterService.getTourPage(
                            effectiveKeyword, tourType, city, destination, days, month, priceRange,
                            searchMode, intentDestination, matchMode, sortType, currentPage, pageSize),
                    () -> tourService.getToursByPage(
                            effectiveKeyword, tourType, city, destination, days, month, priceRange,
                            searchMode, intentDestination, matchMode, sortType, false, currentPage, pageSize)
            ));
        }
        Page<Tour> page = tourService.getToursByPage(
            effectiveKeyword, tourType, city, destination, days, month, priceRange, searchMode, intentDestination, matchMode, sortType, canIncludeInactive, currentPage, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取前台行程筛选项")
    @GetMapping("/filters")
    public Result<?> getTourFilters(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String tourType,
            @RequestParam(defaultValue = "") String city,
            @RequestParam(defaultValue = "") String destination,
            @RequestParam(defaultValue = "") String days,
            @RequestParam(defaultValue = "") String month,
            @RequestParam(defaultValue = "") String priceRange,
            @RequestParam(defaultValue = "") String searchMode,
            @RequestParam(defaultValue = "") String intentDestination,
            @RequestParam(defaultValue = "") String matchMode) {
        String effectiveKeyword = !keyword.isBlank() ? keyword : search;
        if (useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    () -> miniappTourAdapterService.getTourFilters(
                            effectiveKeyword, tourType, city, destination, days, month, priceRange,
                            searchMode, intentDestination, matchMode),
                    () -> tourService.getTourFilters(
                            effectiveKeyword, tourType, city, destination, days, month, priceRange,
                            searchMode, intentDestination, matchMode)
            ));
        }
        return Result.success(tourService.getTourFilters(
            effectiveKeyword, tourType, city, destination, days, month, priceRange, searchMode, intentDestination, matchMode));
    }

    @Operation(summary = "获取前台热门行程关键词")
    @GetMapping("/hot-keywords")
    public Result<?> getHotKeywords() {
        if (useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    () -> miniappTourAdapterService.getHotKeywords(8),
                    () -> tourService.getHotKeywords(8)
            ));
        }
        return Result.success(tourService.getHotKeywords(8));
    }

    @Operation(summary = "获取行程预订页精选推荐")
    @GetMapping("/ticket-featured")
    public Result<?> getTicketFeaturedTours() {
        if (useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    () -> miniappTourAdapterService.getFeaturedTours(4),
                    () -> tourService.getTicketFeaturedTours(4)
            ));
        }
        return Result.success(tourService.getTicketFeaturedTours(4));
    }

    @Operation(summary = "获取所有行程")
    @GetMapping("/all")
    public Result<?> getAllTours() {
        SecurityGuards.requireAnyPermission("tour:manage", "recommend:manage");
        List<Tour> tours = tourService.getAllTours();
        return Result.success(tours);
    }

    @Operation(summary = "获取所有上架行程（前台使用）")
    @GetMapping("/list")
    public Result<?> getActiveTours() {
        if (useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    miniappTourAdapterService::getActiveTours,
                    tourService::getActiveTours
            ));
        }
        List<Tour> tours = tourService.getActiveTours();
        return Result.success(tours);
    }

    // ==================== 首页推荐管理接口 ====================

    @Operation(summary = "获取精选行程（首页使用）")
    @GetMapping("/featured")
    public Result<?> getFeaturedTours() {
        if (useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    () -> miniappTourAdapterService.getFeaturedTours(6),
                    tourService::getFeaturedTours
            ));
        }
        List<Tour> tours = tourService.getFeaturedTours();
        return Result.success(tours);
    }

    @Operation(summary = "获取更多推荐行程（首页使用）")
    @GetMapping("/more")
    public Result<?> getMoreTours() {
        if (useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    () -> miniappTourAdapterService.getMoreTours(6, 24),
                    tourService::getMoreTours
            ));
        }
        List<Tour> tours = tourService.getMoreTours();
        return Result.success(tours);
    }

    @Operation(summary = "获取首页推荐列表（后台管理使用）")
    @GetMapping("/recommends")
    public Result<?> getRecommends(@RequestParam(required = false) String type) {
        SecurityGuards.requirePermission("recommend:manage");
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
    @OperationLog(operationType = "UPDATE", description = "保存首页推荐行程", targetType = "行程")
    public Result<?> saveRecommends(@RequestBody java.util.Map<String, Object> body) {
        SecurityGuards.requirePermission("recommend:manage");
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
    @OperationLog(operationType = "DELETE", description = "清空首页推荐行程", targetType = "行程")
    public Result<?> clearRecommends(@RequestParam String type) {
        SecurityGuards.requirePermission("recommend:manage");
        tourService.clearRecommendsByType(type);
        return Result.success();
    }

    @Operation(summary = "根据ID获取行程详情（简单信息）")
    @GetMapping("/{id}")
    public Result<?> getTourById(@PathVariable String id) {
        if (miniappTourAdapterService.isMiniappTourId(id)) {
            return Result.success(miniappTourAdapterService.getTourDetail(id).get("tour"));
        }
        return Result.success(tourService.getTourById(parseLocalTourId(id)));
    }

    @Operation(summary = "更新外部行程商品的官网展示配置")
    @PutMapping("/source-display")
    @OperationLog(operationType = "UPDATE", description = "更新小程序行程官网展示配置", targetType = "行程")
    public Result<?> updateSourceDisplay(@RequestBody Map<String, Object> body) {
        SecurityGuards.requirePermission("tour:manage");
        String sourceId = body.get("sourceId") == null ? "" : String.valueOf(body.get("sourceId")).trim();
        Object visibleValue = body.get("visible");
        boolean visible = visibleValue instanceof Boolean value
                ? value
                : "1".equals(String.valueOf(visibleValue))
                    || "true".equalsIgnoreCase(String.valueOf(visibleValue));
        int sortOrder;
        try {
            sortOrder = body.get("sortOrder") == null ? 0 : Integer.parseInt(String.valueOf(body.get("sortOrder")));
        } catch (NumberFormatException ex) {
            throw new ServiceException("排序值必须是整数");
        }
        tourSourceDisplayConfigService.save(MiniappTourAdapterService.SOURCE_TYPE, sourceId, visible, sortOrder);
        return Result.success(Map.of(
                "sourceId", sourceId,
                "visible", visible,
                "sortOrder", sortOrder
        ));
    }

    @Operation(summary = "拖拽调整外部行程商品官网顺序")
    @PutMapping("/source-display/reorder")
    @OperationLog(operationType = "UPDATE", description = "拖拽调整小程序行程官网顺序", targetType = "行程")
    public Result<?> reorderSourceDisplay(@RequestBody Map<String, Object> body) {
        SecurityGuards.requirePermission("tour:manage");
        Object sourceIdsValue = body.get("sourceIds");
        if (!(sourceIdsValue instanceof List<?> values)) {
            throw new ServiceException("拖拽排序商品不能为空");
        }
        List<String> sourceIds = values.stream()
                .map(value -> value == null ? "" : String.valueOf(value).trim())
                .toList();
        int startOrder;
        try {
            startOrder = body.get("startOrder") == null ? 1 : Integer.parseInt(String.valueOf(body.get("startOrder")));
        } catch (NumberFormatException ex) {
            throw new ServiceException("拖拽排序起始值必须是整数");
        }
        tourSourceDisplayConfigService.reorder(MiniappTourAdapterService.SOURCE_TYPE, sourceIds, startOrder);
        return Result.success(Map.of("count", sourceIds.size(), "startOrder", startOrder));
    }

    @Operation(summary = "根据ID获取行程完整详情（包含套餐、批次等）")
    @GetMapping("/{id}/detail")
    public Result<?> getTourDetail(@PathVariable String id) {
        if (miniappTourAdapterService.isMiniappTourId(id)) {
            return Result.success(miniappTourAdapterService.getTourDetail(id));
        }
        TourDetailDTO detail = tourService.getTourDetail(parseLocalTourId(id));
        return Result.success(detail);
    }

    @Operation(summary = "新增行程")
    @PostMapping
    @OperationLog(operationType = "CREATE", description = "新增行程", targetType = "行程")
    public Result<?> addTour(@RequestBody Tour tour) {
        SecurityGuards.requirePermission("tour:manage");
        tourService.addTour(tour);
        return Result.success();
    }

    @Operation(summary = "更新行程信息")
    @PutMapping("/{id}")
    @OperationLog(operationType = "UPDATE", description = "更新行程信息", targetType = "行程")
    public Result<?> updateTour(@PathVariable Long id, @RequestBody Tour tour) {
        SecurityGuards.requirePermission("tour:manage");
        tour.setId(id);
        tourService.updateTour(tour);
        return Result.success();
    }

    @Operation(summary = "删除行程")
    @DeleteMapping("/{id}")
    @OperationLog(operationType = "DELETE", description = "删除行程", targetType = "行程")
    public Result<?> deleteTour(@PathVariable Long id) {
        SecurityGuards.requirePermission("tour:manage");
        tourService.deleteTour(id);
        return Result.success();
    }

    @Operation(summary = "更新行程状态")
    @PutMapping("/{id}/status")
    @OperationLog(operationType = "UPDATE_STATUS", description = "更新行程状态", targetType = "行程")
    public Result<?> updateTourStatus(@PathVariable Long id, @RequestParam Integer status) {
        SecurityGuards.requirePermission("tour:manage");
        tourService.updateTourStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "更新行程图片")
    @PutMapping("/{id}/images")
    @OperationLog(operationType = "UPDATE", description = "更新行程图片", targetType = "行程")
    public Result<?> updateTourImages(@PathVariable Long id, @RequestBody java.util.Map<String, List<String>> body) {
        SecurityGuards.requirePermission("tour:manage");
        List<String> images = body.get("images");
        logger.debug("Update tour images request: id={}, imageCount={}", id, images == null ? 0 : images.size());
        tourService.updateTourImages(id, images);
        return Result.success();
    }

    @Operation(summary = "更新行程视频")
    @PutMapping("/{id}/video")
    @OperationLog(operationType = "UPDATE", description = "更新行程视频", targetType = "行程")
    public Result<?> updateTourVideo(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        SecurityGuards.requirePermission("tour:manage");
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
        if (useMiniappSource()) {
            return Result.success(withCatalogFallback(
                    () -> miniappTourAdapterService.getRecommendedTours(scenicName, location, limit == null ? 6 : limit),
                    () -> tourService.getRecommendedToursByScenic(scenicName, location, limit)
            ));
        }
        List<Tour> tours = tourService.getRecommendedToursByScenic(scenicName, location, limit);
        return Result.success(tours);
    }

    @Operation(summary = "删除首页推荐")
    @DeleteMapping("/recommend/{id}")
    @OperationLog(operationType = "DELETE", description = "删除首页推荐行程", targetType = "行程")
    public Result<?> deleteRecommend(@PathVariable Long id) {
        SecurityGuards.requirePermission("recommend:manage");
        tourService.deleteRecommend(id);
        return Result.success();
    }

    @Operation(summary = "更新推荐排序")
    @PutMapping("/recommend/sort")
    @OperationLog(operationType = "UPDATE", description = "更新首页推荐排序", targetType = "行程")
    public Result<?> updateRecommendSort(@RequestBody java.util.Map<String, Object> body) {
        SecurityGuards.requirePermission("recommend:manage");
        @SuppressWarnings("unchecked")
        java.util.List<Number> ids = (java.util.List<Number>) body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("ID列表不能为空");
        }
        tourService.updateRecommendSort(ids);
        return Result.success();
    }

    private boolean useMiniappSource() {
        return tourProductSourceConfigService.isMiniappMode();
    }

    private <T> T withCatalogFallback(Supplier<T> remote, Supplier<T> local) {
        try {
            return remote.get();
        } catch (ServiceException ex) {
            if (!tourProductSourceConfigService.shouldFallbackToLocal()) {
                throw ex;
            }
            logger.warn("Miniapp product source unavailable, fallback to local catalog: {}", ex.getMessage());
            return local.get();
        }
    }

    private Long parseLocalTourId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException ignored) {
            throw new ServiceException("行程不存在");
        }
    }
}
