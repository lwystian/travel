package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.TravelGuideService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "旅游攻略接口")
@RestController
@RequestMapping("/guide")
public class TravelGuideController {
    @Resource
    private TravelGuideService travelGuideService;

    @Operation(summary = "分页查询攻略")
    @GetMapping("/page")
    public Result<?> getGuidesByPage(
        @RequestParam(defaultValue = "") String title,
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "hot") String sortMode,
        @RequestParam(defaultValue = "") String orderBy,
        @RequestParam(defaultValue = "desc") String order,
        @RequestParam(defaultValue = "") String destination,
        @RequestParam(defaultValue = "") String authorRole,
        @RequestParam(required = false) Integer minViews,
        @RequestParam(defaultValue = "") String timeRange,
        @RequestParam(defaultValue = "1") Integer currentPage,
        @RequestParam(defaultValue = "10") Integer size) {
        Page<TravelGuide> page = travelGuideService.getGuidesByPage(
                title, userId, sortMode, orderBy, order, destination, authorRole, minViews, timeRange, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "查看攻略详情")
    @GetMapping("/{id}")
    public Result<?> getGuideById(@PathVariable Long id) {
        TravelGuide guide = travelGuideService.getById(id);
        if (guide == null) {
            throw new ServiceException("攻略不存在");
        }

        // 检查审核状态
        User currentUser = JwtTokenUtils.getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getId().equals(guide.getUserId());
        boolean canReviewOrManage = currentUser != null && canReviewOrManageGuide();

        // 有审核/攻略管理权限或作者本人可以查看未公开攻略
        // 其他用户只能查看审核通过的攻略
        if (!canReviewOrManage && !isOwner && guide.getReviewStatus() != 1) {
            if (guide.getReviewStatus() == 0) {
                throw new ServiceException("该攻略正在审核中，暂不可查看");
            } else {
                throw new ServiceException("该攻略审核未通过，无法查看");
            }
        }

        // 只有审核通过的攻略才增加浏览量
        if (guide.getReviewStatus() == 1) {
            travelGuideService.addView(id);
        }

        return Result.success(guide);
    }

    @Operation(summary = "新增攻略")
    @PostMapping("/add")
    @OperationLog(operationType = "CREATE", description = "发布旅游攻略", targetType = "攻略")
    public Result<?> addGuide(@RequestBody TravelGuide guide) {
        travelGuideService.addGuide(guide);
        return Result.success("攻略发布成功，需审核通过后才能正常显示");
    }

    @Operation(summary = "获取我的攻略列表")
    @GetMapping("/my")
    public Result<?> getMyGuides(
        @RequestParam(defaultValue = "1") Integer currentPage,
        @RequestParam(defaultValue = "10") Integer size) {
        Page<TravelGuide> page = travelGuideService.getMyGuides(currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "修改攻略")
    @PutMapping("/update")
    @OperationLog(operationType = "UPDATE", description = "编辑旅游攻略", targetType = "攻略")
    public Result<?> updateGuide(@RequestBody TravelGuide guide) {
        travelGuideService.updateGuide(guide);
        return Result.success();
    }

    @Operation(summary = "删除攻略")
    @DeleteMapping("/delete/{id}")
    @OperationLog(operationType = "DELETE", description = "删除旅游攻略", targetType = "攻略")
    public Result<?> deleteGuide(@PathVariable Long id) {
        travelGuideService.deleteGuide(id);
        return Result.success();
    }

    // 获取热门攻略
    @Operation(summary = "获取热门攻略")
    @GetMapping("/hot")
    public Result<?> getHotGuides(
            @RequestParam(required = false, defaultValue = "3") Integer limit) {
        List<Map<String, Object>> hotGuides = travelGuideService.getHotGuides(limit);
        return Result.success(hotGuides);
    }

    @Operation(summary = "获取攻略搜索建议")
    @GetMapping("/suggestions")
    public Result<?> getGuideSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") Integer limit) {
        List<Map<String, Object>> suggestions = travelGuideService.getGuideSuggestions(keyword, limit);
        return Result.success(suggestions);
    }

    private boolean canReviewOrManageGuide() {
        try {
            SecurityGuards.requireAnyPermission("guide:manage", "review:manage");
            return true;
        } catch (ServiceException ignored) {
            return false;
        }
    }
}
