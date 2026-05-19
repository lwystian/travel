package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.springboot.entity.SiteNotification;

import java.util.Collection;

public interface SiteNotificationService extends IService<SiteNotification> {
    void sendToUser(Long userId, String title, String content, String type, String businessType, String businessId, String linkUrl);

    void sendToUsers(Collection<Long> userIds, String title, String content, String type, String businessType, String businessId, String linkUrl);

    void sendToAdmins(String title, String content, String type, String businessType, String businessId, String linkUrl);

    Page<SiteNotification> pageForCurrentUser(Integer readStatus, Integer currentPage, Integer size);

    long unreadCount();

    void markRead(Long id);

    void markAllRead();
}
