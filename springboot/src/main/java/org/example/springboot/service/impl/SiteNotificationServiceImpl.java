package org.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.springboot.entity.SiteNotification;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.SiteNotificationMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.service.SiteNotificationService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class SiteNotificationServiceImpl extends ServiceImpl<SiteNotificationMapper, SiteNotification> implements SiteNotificationService {
    @Resource
    private UserMapper userMapper;

    @Override
    public void sendToUser(Long userId, String title, String content, String type, String businessType, String businessId, String linkUrl) {
        if (userId == null) {
            return;
        }
        SiteNotification notification = build(userId, title, content, type, businessType, businessId, linkUrl, getCurrentUserQuietly());
        this.save(notification);
    }

    @Override
    public void sendToUsers(Collection<Long> userIds, String title, String content, String type, String businessType, String businessId, String linkUrl) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        User sender = getCurrentUserQuietly();
        for (Long userId : userIds) {
            SiteNotification notification = build(userId, title, content, type, businessType, businessId, linkUrl, sender);
            this.save(notification);
        }
    }

    @Override
    public void sendToAdmins(String title, String content, String type, String businessType, String businessId, String linkUrl) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getRoleCode, List.of(RolePermission.SUPER_ADMIN, RolePermission.ADMIN)).eq(User::getStatus, 1);
        List<Long> adminIds = userMapper.selectList(wrapper).stream().map(User::getId).toList();
        sendToUsers(adminIds, title, content, type, businessType, businessId, linkUrl);
    }

    @Override
    public Page<SiteNotification> pageForCurrentUser(Integer readStatus, Integer currentPage, Integer size) {
        User currentUser = requireCurrentUser();
        LambdaQueryWrapper<SiteNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SiteNotification::getUserId, currentUser.getId())
                .eq(SiteNotification::getStatus, 1)
                .eq(readStatus != null, SiteNotification::getReadStatus, readStatus)
                .orderByAsc(SiteNotification::getReadStatus)
                .orderByDesc(SiteNotification::getCreateTime);
        return this.page(new Page<>(currentPage, size), wrapper);
    }

    @Override
    public long unreadCount() {
        User currentUser = requireCurrentUser();
        return this.lambdaQuery()
                .eq(SiteNotification::getUserId, currentUser.getId())
                .eq(SiteNotification::getReadStatus, 0)
                .eq(SiteNotification::getStatus, 1)
                .count();
    }

    @Override
    public void markRead(Long id) {
        User currentUser = requireCurrentUser();
        this.lambdaUpdate()
                .eq(SiteNotification::getId, id)
                .eq(SiteNotification::getUserId, currentUser.getId())
                .set(SiteNotification::getReadStatus, 1)
                .set(SiteNotification::getReadTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void markAllRead() {
        User currentUser = requireCurrentUser();
        this.lambdaUpdate()
                .eq(SiteNotification::getUserId, currentUser.getId())
                .eq(SiteNotification::getReadStatus, 0)
                .set(SiteNotification::getReadStatus, 1)
                .set(SiteNotification::getReadTime, LocalDateTime.now())
                .update();
    }

    private SiteNotification build(Long userId, String title, String content, String type, String businessType, String businessId, String linkUrl, User sender) {
        SiteNotification notification = new SiteNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type == null ? "SYSTEM" : type);
        notification.setBusinessType(businessType);
        notification.setBusinessId(businessId);
        notification.setLinkUrl(linkUrl);
        notification.setReadStatus(0);
        notification.setSenderType(sender == null ? "SYSTEM" : RolePermission.isAdmin(sender) ? "ADMIN" : "USER");
        notification.setSenderId(sender == null ? null : sender.getId());
        notification.setSenderName(sender == null ? "系统" : sender.getUsername());
        notification.setStatus(1);
        notification.setCreateTime(LocalDateTime.now());
        return notification;
    }

    private User requireCurrentUser() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录后查看站内消息");
        }
        return currentUser;
    }

    private User getCurrentUserQuietly() {
        try {
            return JwtTokenUtils.getCurrentUser();
        } catch (Exception ignored) {
            return null;
        }
    }
}
