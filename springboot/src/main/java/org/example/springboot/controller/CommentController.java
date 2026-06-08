package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.security.RolePermission;
import org.example.springboot.service.CommentLikeService;
import org.example.springboot.service.CommentService;
import org.example.springboot.service.ContentModerationConfigService;
import org.example.springboot.service.ScenicSpotService;
import org.example.springboot.service.UserService;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.RequestMetadataUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "评论管理接口")
@RestController
@RequestMapping("/comment")
public class CommentController {
    @Resource
    private CommentService commentService;
    @Resource
    private UserService userService;
    @Resource
    private CommentLikeService commentLikeService;
    @Resource
    private ScenicSpotService scenicSpotService;
    @Resource
    private ContentModerationConfigService contentModerationConfigService;

    @Operation(summary = "分页查询评论")
    @GetMapping("/page")
    public Result<?> getCommentsByPage(
            @RequestParam(required = false) Long scenicId,
            @RequestParam(required = false) String scenicName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String content,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Comment> page = commentService.getCommentsByPage(scenicId, scenicName, userName, content, currentPage, size);
        // 批量查用户
        List<Long> userIds = page.getRecords().stream().map(Comment::getUserId).distinct().toList();
        List<User> users = userService.getUsersByIds(userIds);
        
        // 处理用户信息
        for (Comment c : page.getRecords()) {
            users.stream()
                .filter(u -> u.getId().equals(c.getUserId()))
                .findFirst()
                .ifPresent(u -> {
                    fillCommentUserInfo(c, u);
                });
        }
        
        // 批量查询景点信息
        List<Long> scenicIds = page.getRecords().stream().map(Comment::getScenicId).distinct().toList();
        List<ScenicSpot> scenicSpots = scenicSpotService.getScenicSpotsByIds(scenicIds);
        for (Comment c : page.getRecords()) {
            scenicSpots.stream()
                .filter(s -> s.getId().equals(c.getScenicId()))
                .findFirst()
                .ifPresent(s -> c.setScenicName(s.getName()));
        }
        
        // 批量查询点赞状态
        if (!page.getRecords().isEmpty() && JwtTokenUtils.getCurrentUser() != null) {
            List<Long> commentIds = page.getRecords().stream().map(Comment::getId).toList();
            List<Long> likedIds = commentLikeService.batchCheckLiked(commentIds);
            
            // 将liked标记添加到每个评论中
            Map<Long, Boolean> likedMap = likedIds.stream().collect(Collectors.toMap(id -> id, id -> true));
            for (Comment c : page.getRecords()) {
                c.setLiked(likedMap.getOrDefault(c.getId(), false));
            }
        }
        
        return Result.success(page);
    }

    @Operation(summary = "添加评论")
    @PostMapping("/add")
    @OperationLog(operationType = "CREATE", description = "发布景点评论", targetType = "评论")
    public Result<?> addComment(@RequestBody Comment comment, HttpServletRequest request) {
        contentModerationConfigService.requirePublicInteractionEnabled();
        fillRequestMetadata(comment, request);
        commentService.addComment(comment);
        return Result.success(comment.getReviewStatus() != null && comment.getReviewStatus() == 1
                ? "评论发布成功"
                : "评论成功，需审核通过后才能正常显示");
    }

    private void fillRequestMetadata(Comment comment, HttpServletRequest request) {
        comment.setIpAddress(RequestMetadataUtil.clientIp(request));
        comment.setPort(RequestMetadataUtil.clientPort(request));
        comment.setUserAgent(RequestMetadataUtil.userAgent(request));
        comment.setDeviceId(RequestMetadataUtil.deviceId(request));
        comment.setDeviceFingerprint(RequestMetadataUtil.deviceFingerprint(request));
        comment.setClientHardware(RequestMetadataUtil.clientHardware(request));
        comment.setMacAddress(RequestMetadataUtil.macAddress(request));
    }

    @Operation(summary = "获取我的评论列表")
    @GetMapping("/my")
    public Result<?> getMyComments(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = JwtTokenUtils.getCurrentUser().getId();
        Page<Comment> page = commentService.getMyComments(userId, currentPage, size);

        // 批量查询景点信息
        List<Long> scenicIds = page.getRecords().stream().map(Comment::getScenicId).distinct().toList();
        List<ScenicSpot> scenicSpots = scenicSpotService.getScenicSpotsByIds(scenicIds);
        for (Comment c : page.getRecords()) {
            scenicSpots.stream()
                .filter(s -> s.getId().equals(c.getScenicId()))
                .findFirst()
                .ifPresent(s -> c.setScenicName(s.getName()));
        }

        return Result.success(page);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/delete/{id}")
    @OperationLog(operationType = "DELETE", description = "删除景点评论", targetType = "评论")
    public Result<?> deleteComment(@PathVariable Long id) {
        var user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        Comment comment = commentService.getById(id);
        boolean isOwner = comment.getUserId() != null && comment.getUserId().equals(user.getId());
        boolean isAdmin = !isOwner && RolePermission.isAdmin(user);
        if (isAdmin) {
            SecurityGuards.requirePermission("review:manage");
        }
        commentService.deleteComment(id, user.getId(), isAdmin);
        return Result.success("删除成功");
    }

    @Operation(summary = "点赞/取消点赞评论")
    @PutMapping("/like/{id}")
    @OperationLog(operationType = "LIKE", description = "点赞或取消点赞评论", targetType = "评论")
    public Result<?> toggleLike(@PathVariable Long id) {
        contentModerationConfigService.requirePublicInteractionEnabled();
        boolean isLiked = commentLikeService.toggleLike(id);
        return Result.success(isLiked ? "点赞成功" : "取消点赞成功", isLiked);
    }

    @Operation(summary = "获取评论详情")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        Comment comment = commentService.getById(id);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        // 检查审核状态
        User currentUser = JwtTokenUtils.getCurrentUser();
        boolean isAdmin = RolePermission.isAdmin(currentUser);
        boolean isOwner = currentUser != null && currentUser.getId().equals(comment.getUserId());

        // 管理员或作者本人可以查看所有评论
        // 其他用户只能查看审核通过的评论
        if (!isAdmin && !isOwner && comment.getReviewStatus() != 1) {
            if (comment.getReviewStatus() == 0) {
                throw new ServiceException("该评论正在审核中，暂不可查看");
            } else {
                throw new ServiceException("该评论审核未通过，无法查看");
            }
        }

        return Result.success(comment);
    }

    @Operation(summary = "获取某景点所有评论")
    @GetMapping("/scenic/{scenicId}")
    public Result<?> getAllByScenicId(@PathVariable Long scenicId) {
        List<Comment> list = commentService.getAllByScenicId(scenicId);
        List<Long> userIds = list.stream().map(Comment::getUserId).distinct().toList();
        List<User> users = userService.getUsersByIds(userIds);
        for (Comment c : list) {
            users.stream()
                 .filter(u -> u.getId().equals(c.getUserId()))
                 .findFirst()
                 .ifPresent(u -> {
                     fillCommentUserInfo(c, u);
                 });
        }
        
        // 添加景点名称
        ScenicSpot scenicSpot = scenicSpotService.getById(scenicId);
        if (scenicSpot != null) {
            for (Comment c : list) {
                c.setScenicName(scenicSpot.getName());
            }
        }
        
        // 批量查询点赞状态
        if (!list.isEmpty() && JwtTokenUtils.getCurrentUser() != null) {
            List<Long> commentIds = list.stream().map(Comment::getId).toList();
            List<Long> likedIds = commentLikeService.batchCheckLiked(commentIds);
            
            // 将liked标记添加到每个评论中
            Map<Long, Boolean> likedMap = likedIds.stream().collect(Collectors.toMap(id -> id, id -> true));
            for (Comment c : list) {
                c.setLiked(likedMap.getOrDefault(c.getId(), false));
            }
        }
        
        return Result.success(list);
    }
    
    @Operation(summary = "检查评论是否已点赞")
    @GetMapping("/isLiked/{id}")
    public Result<?> isLiked(@PathVariable Long id) {
        if (!contentModerationConfigService.isPublicInteractionEnabled()) {
            return Result.success(false);
        }
        boolean liked = commentLikeService.isLiked(id);
        return Result.success(liked);
    }

    private void fillCommentUserInfo(Comment comment, User user) {
        comment.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        comment.setUserAvatar(user.getAvatar());
        String roleCode = RolePermission.normalizeRole(user.getRoleCode());
        comment.setUserRoleCode(roleCode);
        comment.setUserRoleName(RolePermission.roleNameOf(roleCode));
    }
} 
