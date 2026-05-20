package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.SiteNotification;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.service.SiteNotificationService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notification")
public class SiteNotificationController {
    @Resource
    private SiteNotificationService siteNotificationService;
    @Resource
    private UserMapper userMapper;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(required = false) Integer readStatus,
                          @RequestParam(defaultValue = "1") Integer currentPage,
                          @RequestParam(defaultValue = "10") Integer size) {
        Page<SiteNotification> page = siteNotificationService.pageForCurrentUser(readStatus, currentPage, size);
        return Result.success(page);
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        return Result.success(siteNotificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    @OperationLog(operationType = "UPDATE", description = "标记站内消息已读", targetType = "站内消息")
    public Result<?> markRead(@PathVariable Long id) {
        siteNotificationService.markRead(id);
        return Result.success("已标记为已读");
    }

    @PutMapping("/read-all")
    @OperationLog(operationType = "UPDATE", description = "全部站内消息已读", targetType = "站内消息")
    public Result<?> markAllRead() {
        siteNotificationService.markAllRead();
        return Result.success("已全部标记为已读");
    }

    @PostMapping("/admin/send")
    @OperationLog(operationType = "CREATE", description = "发送站内消息", targetType = "站内消息")
    public Result<?> adminSend(@RequestBody Map<String, Object> body) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!RolePermission.isAdmin(currentUser)) {
            return Result.error("无权限发送站内消息");
        }
        String title = String.valueOf(body.getOrDefault("title", "系统通知"));
        String content = String.valueOf(body.getOrDefault("content", ""));
        String target = String.valueOf(body.getOrDefault("target", "ALL"));
        if ("ADMIN".equals(target)) {
            siteNotificationService.sendToAdmins(title, content, "ADMIN", "ADMIN_NOTICE", null, null);
        } else if ("USER".equals(target)) {
            List<Long> userIds = userMapper.selectList(null).stream()
                    .filter(user -> !RolePermission.isAdmin(user))
                    .map(User::getId)
                    .toList();
            siteNotificationService.sendToUsers(userIds, title, content, "ADMIN", "ADMIN_NOTICE", null, null);
        } else {
            List<Long> userIds = userMapper.selectList(null).stream().map(User::getId).toList();
            siteNotificationService.sendToUsers(userIds, title, content, "ADMIN", "ADMIN_NOTICE", null, null);
        }
        return Result.success("站内消息已发送");
    }

    @GetMapping("/admin/page")
    public Result<?> adminPage(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "") String readStatus,
                               @RequestParam(defaultValue = "1") Integer currentPage,
                               @RequestParam(defaultValue = "10") Integer size) {
        if (!isAdmin()) {
            return Result.error("无权限查看站内消息记录");
        }
        Integer parsedReadStatus = parseInteger(readStatus);
        LambdaQueryWrapper<SiteNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SiteNotification::getStatus, 1)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SiteNotification::getTitle, keyword)
                        .or()
                        .like(SiteNotification::getContent, keyword))
                .eq(parsedReadStatus != null, SiteNotification::getReadStatus, parsedReadStatus)
                .orderByDesc(SiteNotification::getCreateTime);
        Page<SiteNotification> page = siteNotificationService.page(new Page<>(currentPage, size), wrapper);
        fillReceiverNames(page.getRecords());
        return Result.success(page);
    }

    @DeleteMapping("/admin/{id}")
    @OperationLog(operationType = "DELETE", description = "删除站内消息记录", targetType = "站内消息")
    public Result<?> adminDelete(@PathVariable Long id) {
        if (!isAdmin()) {
            return Result.error("无权限删除站内消息记录");
        }
        SiteNotification update = new SiteNotification();
        update.setId(id);
        update.setStatus(0);
        siteNotificationService.updateById(update);
        return Result.success("站内消息记录已删除");
    }

    @DeleteMapping("/admin/batch")
    @OperationLog(operationType = "DELETE", description = "批量删除站内消息记录", targetType = "站内消息")
    public Result<?> adminBatchDelete(@RequestBody List<Long> ids) {
        if (!isAdmin()) {
            return Result.error("无权限删除站内消息记录");
        }
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择需要删除的站内消息");
        }
        for (Long id : ids) {
            SiteNotification update = new SiteNotification();
            update.setId(id);
            update.setStatus(0);
            siteNotificationService.updateById(update);
        }
        return Result.success("站内消息记录已批量删除");
    }

    private boolean isAdmin() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        return RolePermission.isAdmin(currentUser);
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void fillReceiverNames(List<SiteNotification> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream()
                .map(SiteNotification::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));
        for (SiteNotification record : records) {
            User receiver = userMap.get(record.getUserId());
            if (receiver == null) {
                record.setReceiverName("用户#" + record.getUserId());
            } else if (receiver.getNickname() != null && !receiver.getNickname().isBlank()) {
                record.setReceiverName(receiver.getNickname() + "（" + receiver.getUsername() + "）");
            } else {
                record.setReceiverName(receiver.getUsername());
            }
        }
    }
}
