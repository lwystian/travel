package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.AccommodationReview;
import org.example.springboot.dto.ReviewRequestDTO;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.service.ContentReviewService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 内容审核控制器
 * 统一管理评论和攻略的审核
 */
@Tag(name = "内容审核管理")
@RestController
@RequestMapping("/review")
public class ReviewController {
    
    @Resource
    private ContentReviewService contentReviewService;
    
    // ==================== 评论审核 ====================
    
    @Operation(summary = "分页查询待审核评论")
    @GetMapping("/comment/pending")
    @OperationLog(operationType = "QUERY", description = "查询待审核评论")
    public Result<?> getPendingComments(
            @RequestParam(defaultValue = "") String scenicName,
            @RequestParam(defaultValue = "") String userName,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        
        checkAdminPermission();
        Page<Comment> page = contentReviewService.getPendingComments(scenicName, userName, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "分页查询所有评论（管理员）")
    @GetMapping("/comment/all")
    @OperationLog(operationType = "QUERY", description = "查询所有评论")
    public Result<?> getAllComments(
            @RequestParam(defaultValue = "") String scenicName,
            @RequestParam(defaultValue = "") String userName,
            @RequestParam(required = false) Integer reviewStatus,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        
        checkAdminPermission();
        Page<Comment> page = contentReviewService.getAllComments(scenicName, userName, reviewStatus, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "审核评论")
    @PostMapping("/comment/{id}")
    @OperationLog(operationType = "UPDATE", description = "审核评论")
    public Result<?> reviewComment(
            @PathVariable Long id,
            @RequestBody ReviewRequestDTO request) {
        
        User currentUser = checkAdminPermission();
        contentReviewService.reviewComment(id, request.getStatus(), currentUser.getId(), 
                currentUser.getUsername(), request.getReviewComment());
        return Result.success("审核完成");
    }
    
    @Operation(summary = "批量审核评论")
    @PostMapping("/comment/batch")
    @OperationLog(operationType = "UPDATE", description = "批量审核评论")
    public Result<?> batchReviewComments(
            @RequestBody Map<String, Object> request) {
        
        User currentUser = checkAdminPermission();
        Long[] ids = parseIds(request.get("ids"));
        Integer status = request.get("status") != null ? Integer.valueOf(request.get("status").toString()) : null;
        String reviewComment = request.get("reviewComment") != null ? request.get("reviewComment").toString() : null;
        
        if (ids != null) {
            for (Long id : ids) {
                try {
                    contentReviewService.reviewComment(id, status, currentUser.getId(), 
                            currentUser.getUsername(), reviewComment);
                } catch (Exception e) {
                    // 继续处理其他评论
                }
            }
        }
        return Result.success("批量审核完成");
    }

    // ==================== 住宿评价审核 ====================

    @Operation(summary = "分页查询待审核住宿评价")
    @GetMapping("/accommodation/pending")
    @OperationLog(operationType = "QUERY", description = "查询待审核住宿评价")
    public Result<?> getPendingAccommodationReviews(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        checkAdminPermission();
        Page<AccommodationReview> page = contentReviewService.getPendingAccommodationReviews(currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "分页查询所有住宿评价")
    @GetMapping("/accommodation/all")
    @OperationLog(operationType = "QUERY", description = "查询所有住宿评价")
    public Result<?> getAllAccommodationReviews(
            @RequestParam(required = false) Integer reviewStatus,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        checkAdminPermission();
        Page<AccommodationReview> page = contentReviewService.getAllAccommodationReviews(reviewStatus, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "审核住宿评价")
    @PostMapping("/accommodation/{id}")
    @OperationLog(operationType = "UPDATE", description = "审核住宿评价")
    public Result<?> reviewAccommodationReview(
            @PathVariable Long id,
            @RequestBody ReviewRequestDTO request) {
        User currentUser = checkAdminPermission();
        contentReviewService.reviewAccommodationReview(id, request.getStatus(), currentUser.getId(),
                currentUser.getUsername(), request.getReviewComment());
        return Result.success("审核完成");
    }

    @Operation(summary = "批量审核住宿评价")
    @PostMapping("/accommodation/batch")
    @OperationLog(operationType = "UPDATE", description = "批量审核住宿评价")
    public Result<?> batchReviewAccommodationReviews(@RequestBody Map<String, Object> request) {
        User currentUser = checkAdminPermission();
        Long[] ids = parseIds(request.get("ids"));
        Integer status = request.get("status") != null ? Integer.valueOf(request.get("status").toString()) : null;
        String reviewComment = request.get("reviewComment") != null ? request.get("reviewComment").toString() : null;

        if (ids != null) {
            for (Long id : ids) {
                try {
                    contentReviewService.reviewAccommodationReview(id, status, currentUser.getId(),
                            currentUser.getUsername(), reviewComment);
                } catch (Exception e) {
                    // 继续处理其他住宿评价
                }
            }
        }
        return Result.success("批量审核完成");
    }
    
    // ==================== 攻略审核 ====================
    
    @Operation(summary = "分页查询待审核攻略")
    @GetMapping("/guide/pending")
    @OperationLog(operationType = "QUERY", description = "查询待审核攻略")
    public Result<?> getPendingGuides(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String userName,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        
        checkAdminPermission();
        Page<TravelGuide> page = contentReviewService.getPendingGuides(title, userName, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "分页查询所有攻略（管理员）")
    @GetMapping("/guide/all")
    @OperationLog(operationType = "QUERY", description = "查询所有攻略")
    public Result<?> getAllGuides(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(required = false) Integer reviewStatus,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        
        checkAdminPermission();
        Page<TravelGuide> page = contentReviewService.getAllGuides(title, reviewStatus, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "审核攻略")
    @PostMapping("/guide/{id}")
    @OperationLog(operationType = "UPDATE", description = "审核攻略")
    public Result<?> reviewGuide(
            @PathVariable Long id,
            @RequestBody ReviewRequestDTO request) {
        
        User currentUser = checkAdminPermission();
        contentReviewService.reviewGuide(id, request.getStatus(), currentUser.getId(), 
                currentUser.getUsername(), request.getReviewComment());
        return Result.success("审核完成");
    }
    
    @Operation(summary = "批量审核攻略")
    @PostMapping("/guide/batch")
    @OperationLog(operationType = "UPDATE", description = "批量审核攻略")
    public Result<?> batchReviewGuides(
            @RequestBody Map<String, Object> request) {
        
        User currentUser = checkAdminPermission();
        Long[] ids = parseIds(request.get("ids"));
        Integer status = request.get("status") != null ? Integer.valueOf(request.get("status").toString()) : null;
        String reviewComment = request.get("reviewComment") != null ? request.get("reviewComment").toString() : null;
        
        if (ids != null) {
            for (Long id : ids) {
                try {
                    contentReviewService.reviewGuide(id, status, currentUser.getId(), 
                            currentUser.getUsername(), reviewComment);
                } catch (Exception e) {
                    // 继续处理其他攻略
                }
            }
        }
        return Result.success("批量审核完成");
    }
    
    // ==================== 统计 ====================
    
    @Operation(summary = "获取审核统计信息")
    @GetMapping("/stats")
    public Result<?> getReviewStats() {
        return Result.success(contentReviewService.getReviewStats());
    }
    
    @Operation(summary = "获取待审核数量（用于前端徽章显示）")
    @GetMapping("/pending/count")
    public Result<?> getPendingCount() {
        Map<String, Object> count = new java.util.HashMap<>();
        count.put("comment", contentReviewService.getPendingCommentCount());
        count.put("guide", contentReviewService.getPendingGuideCount());
        count.put("accommodation", contentReviewService.getPendingAccommodationReviewCount());
        count.put("total", contentReviewService.getPendingCommentCount() + contentReviewService.getPendingGuideCount()
                + contentReviewService.getPendingAccommodationReviewCount());
        return Result.success(count);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 检查管理员权限
     */
    private User checkAdminPermission() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new org.example.springboot.exception.ServiceException("请先登录");
        }
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new org.example.springboot.exception.ServiceException("无权限访问，需要管理员权限");
        }
        return currentUser;
    }
    
    /**
     * 解析ID数组
     */
    private Long[] parseIds(Object idsObj) {
        if (idsObj == null) return null;
        if (idsObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) idsObj;
            Long[] ids = new Long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Number) {
                    ids[i] = ((Number) item).longValue();
                } else {
                    ids[i] = Long.valueOf(item.toString());
                }
            }
            return ids;
        }
        return null;
    }
}
