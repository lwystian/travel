package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TravelGuideMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.security.SecurityValidationUtil;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.RequestMetadataUtil;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class TravelGuideService {
    @Resource
    private TravelGuideMapper travelGuideMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private SensitiveWordService sensitiveWordService;
    @Resource
    private ContentModerationConfigService contentModerationConfigService;
    @Resource
    private AdminPermissionService adminPermissionService;

    public Page<TravelGuide> getGuidesByPage(String title, Long userId, String sortMode, String orderBy, String order,
                                             String destination, String authorRole, Integer minViews, String timeRange,
                                             Integer currentPage, Integer size) {
        currentPage = SecurityValidationUtil.clampPage(currentPage);
        size = SecurityValidationUtil.clampLimit(size, 10, 100);
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询已审核通过的内容
        queryWrapper.eq(TravelGuide::getReviewStatus, 1);

        // 如果有标题搜索，进行综合搜索（标题、内容）
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.and(wrapper -> wrapper
                .like(TravelGuide::getTitle, title)
                .or()
                .like(TravelGuide::getContent, title)
            );
        }

        if (userId != null) {
            queryWrapper.eq(TravelGuide::getUserId, userId);
        }

        if (StringUtils.isNotBlank(destination)) {
            queryWrapper.like(TravelGuide::getDestination, destination.trim());
        }
        if (minViews != null && minViews > 0) {
            queryWrapper.ge(TravelGuide::getViews, minViews);
        }
        LocalDateTime startTime = resolveStartTime(timeRange);
        if (startTime != null) {
            queryWrapper.ge(TravelGuide::getCreateTime, startTime);
        }
        applyAuthorRoleFilter(queryWrapper, authorRole);
        applyGuideSorting(queryWrapper, sortMode, orderBy, order);

        Page<TravelGuide> page = travelGuideMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        // 批量查用户昵称和头像
        List<Long> userIds = page.getRecords().stream().map(TravelGuide::getUserId).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
            for (TravelGuide guide : page.getRecords()) {
                User user = userMap.get(guide.getUserId());
                if (user != null) {
                    fillGuideUserInfo(guide, user);
                }
            }
        }
        return page;
    }

    private LocalDateTime resolveStartTime(String timeRange) {
        if (StringUtils.isBlank(timeRange)) {
            return null;
        }
        return switch (timeRange.trim()) {
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "quarter" -> LocalDateTime.now().minusMonths(3);
            case "year" -> LocalDateTime.now().minusYears(1);
            default -> null;
        };
    }

    private void applyAuthorRoleFilter(LambdaQueryWrapper<TravelGuide> queryWrapper, String authorRole) {
        if (StringUtils.isBlank(authorRole) || "all".equalsIgnoreCase(authorRole)) {
            return;
        }
        if ("official".equalsIgnoreCase(authorRole)) {
            queryWrapper.inSql(TravelGuide::getUserId,
                    "SELECT id FROM user WHERE UPPER(role_code) IN ('SUPER_ADMIN','ADMIN')");
            return;
        }
        String role = RolePermission.normalizeRole(authorRole);
        if (!List.of(RolePermission.USER).contains(role)) {
            return;
        }
        queryWrapper.inSql(TravelGuide::getUserId,
                "SELECT id FROM user WHERE UPPER(role_code) = '" + role + "'");
    }

    private void applyGuideSorting(LambdaQueryWrapper<TravelGuide> queryWrapper, String sortMode, String orderBy, String order) {
        String mode = StringUtils.isNotBlank(sortMode) ? sortMode.trim().toLowerCase() : "";
        if ("recommended".equals(mode) || "recommend".equals(mode)) {
            queryWrapper.last("ORDER BY CASE WHEN user_id IN (SELECT id FROM user WHERE UPPER(role_code) IN ('SUPER_ADMIN','ADMIN')) THEN 0 ELSE 1 END, views DESC, update_time DESC, create_time DESC");
            return;
        }
        if ("latest".equals(mode)) {
            queryWrapper.orderByDesc(TravelGuide::getCreateTime);
            return;
        }
        if ("hot".equals(mode)) {
            queryWrapper.orderByDesc(TravelGuide::getViews).orderByDesc(TravelGuide::getCreateTime);
            return;
        }

        boolean asc = "asc".equalsIgnoreCase(order);
        String field = StringUtils.isNotBlank(orderBy) ? orderBy.trim() : "createTime";
        switch (field) {
            case "views" -> queryWrapper.orderBy(true, asc, TravelGuide::getViews)
                    .orderByDesc(TravelGuide::getCreateTime);
            case "updateTime" -> queryWrapper.orderBy(true, asc, TravelGuide::getUpdateTime)
                    .orderByDesc(TravelGuide::getCreateTime);
            case "createTime" -> queryWrapper.orderBy(true, asc, TravelGuide::getCreateTime);
            default -> queryWrapper.orderByDesc(TravelGuide::getCreateTime);
        }
    }

    public TravelGuide getById(Long id) {
        TravelGuide guide = travelGuideMapper.selectById(id);
        if (guide != null) {
            User user = userMapper.selectById(guide.getUserId());
            if (user != null) {
                fillGuideUserInfo(guide, user);
            }
        }

        return guide;
    }

    public void addGuide(TravelGuide guide) {
        User currentUser = requireCurrentUser();
        guide.setId(null);
        guide.setUserId(currentUser.getId());
        guide.setReviewerId(null);
        guide.setReviewerName(null);
        guide.setReviewTime(null);
        guide.setReviewComment(null);
        applyGuideModeration(guide, currentUser);
        fillRequestMetadata(guide);
        travelGuideMapper.insert(guide);
    }

    public void updateGuide(TravelGuide guide) {
        if (guide == null || guide.getId() == null) {
            throw new ServiceException("攻略不存在");
        }
        TravelGuide existing = travelGuideMapper.selectById(guide.getId());
        if (existing == null) {
            throw new ServiceException("攻略不存在");
        }
        User currentUser = requireCurrentUser();
        requireOwnerOrAdmin(existing, currentUser);
        guide.setUserId(existing.getUserId());
        guide.setReviewerId(null);
        guide.setReviewerName(null);
        guide.setReviewTime(null);
        guide.setReviewComment(null);
        applyGuideModeration(guide, currentUser);
        fillRequestMetadata(guide);
        travelGuideMapper.updateById(guide);
    }

    private void fillRequestMetadata(TravelGuide guide) {
        HttpServletRequest request = RequestMetadataUtil.currentRequest();
        guide.setIpAddress(RequestMetadataUtil.clientIp(request));
        guide.setPort(RequestMetadataUtil.clientPort(request));
        guide.setUserAgent(RequestMetadataUtil.userAgent(request));
        guide.setDeviceId(RequestMetadataUtil.deviceId(request));
        guide.setDeviceFingerprint(RequestMetadataUtil.deviceFingerprint(request));
        guide.setClientHardware(RequestMetadataUtil.clientHardware(request));
        guide.setMacAddress(RequestMetadataUtil.macAddress(request));
    }

    private void filterGuideContent(TravelGuide guide) {
        String objectId = guide.getId() == null ? null : String.valueOf(guide.getId());
        guide.setTitle(sensitiveWordService.filterContent(guide.getTitle(), "GUIDE_TITLE", objectId));
        guide.setDestination(sensitiveWordService.filterContent(guide.getDestination(), "GUIDE_DESTINATION", objectId));
        guide.setContent(sensitiveWordService.filterContent(guide.getContent(), "GUIDE", objectId));
    }

    private void applyGuideModeration(TravelGuide guide, User currentUser) {
        String roleCode = RolePermission.normalizeRole(currentUser.getRoleCode());
        if (RolePermission.SUPER_ADMIN.equals(roleCode)) {
            guide.setReviewStatus(ContentReviewService.STATUS_APPROVED);
            return;
        }
        if (RolePermission.ADMIN.equals(roleCode)) {
            guide.setReviewStatus(contentModerationConfigService.adminGuideReviewRequired()
                    ? ContentReviewService.STATUS_PENDING
                    : ContentReviewService.STATUS_APPROVED);
            return;
        }
        filterGuideContent(guide);
        guide.setReviewStatus(ContentReviewService.STATUS_PENDING);
    }

    private void fillGuideUserInfo(TravelGuide guide, User user) {
        guide.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        guide.setUserAvatar(user.getAvatar());
        String roleCode = RolePermission.normalizeRole(user.getRoleCode());
        guide.setUserRoleCode(roleCode);
        guide.setUserRoleName(RolePermission.roleNameOf(roleCode));
    }

    public void deleteGuide(Long id) {
        TravelGuide existing = travelGuideMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("攻略不存在");
        }
        User currentUser = requireCurrentUser();
        requireOwnerOrAdmin(existing, currentUser);
        travelGuideMapper.deleteById(id);
    }

    private User requireCurrentUser() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new ServiceException("请先登录");
        }
        return currentUser;
    }

    private void requireOwnerOrAdmin(TravelGuide guide, User currentUser) {
        boolean isOwner = guide.getUserId() != null && guide.getUserId().equals(currentUser.getId());
        if (isOwner) {
            return;
        }
        if (!RolePermission.isAdmin(currentUser) || !adminPermissionService.hasPermission(currentUser, "guide:manage")) {
            throw new ServiceException("权限不足，请联系管理员");
        }
    }

    public void addView(Long id) {
        travelGuideMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TravelGuide>().eq("id", id).setSql("views = views + 1"));
    }

    public Page<TravelGuide> getMyGuides(Integer currentPage, Integer size) {
        currentPage = SecurityValidationUtil.clampPage(currentPage);
        size = SecurityValidationUtil.clampLimit(size, 10, 100);
        Long userId = JwtTokenUtils.getCurrentUser().getId();
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TravelGuide::getUserId, userId);
        queryWrapper.orderByDesc(TravelGuide::getCreateTime);
        return travelGuideMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }

    /**
     * 获取热门攻略列表
     * @param limit 限制数量
     * @return 热门攻略列表
     */
    public List<Map<String, Object>> getHotGuides(Integer limit) {
        limit = SecurityValidationUtil.clampLimit(limit, 3, 50);
        // 根据浏览量倒序排序获取热门攻略
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TravelGuide::getReviewStatus, ContentReviewService.STATUS_APPROVED);
        queryWrapper.orderByDesc(TravelGuide::getViews);
        queryWrapper.last("LIMIT " + limit);
        List<TravelGuide> guides = travelGuideMapper.selectList(queryWrapper);
        
        // 组装返回数据，包含用户信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (TravelGuide guide : guides) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", guide.getId());
            map.put("title", guide.getTitle());
            map.put("coverImage", guide.getCoverImage());
            map.put("views", guide.getViews());
            map.put("createTime", guide.getCreateTime());
            map.put("destination", guide.getDestination());
            
            // 获取用户信息
            if (guide.getUserId() != null) {
                User user = userMapper.selectById(guide.getUserId());
                if (user != null) {
                    map.put("userNickname", user.getNickname());
                    map.put("userAvatar", user.getAvatar());
                    String roleCode = RolePermission.normalizeRole(user.getRoleCode());
                    map.put("userRoleCode", roleCode);
                    map.put("userRoleName", RolePermission.roleNameOf(roleCode));
                }
            }
            
            result.add(map);
        }
        
        return result;
    }

    /**
     * 获取攻略搜索建议
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 搜索建议列表
     */
    public List<Map<String, Object>> getGuideSuggestions(String keyword, Integer limit) {
        limit = SecurityValidationUtil.clampLimit(limit, 5, 20);
        List<Map<String, Object>> result = new ArrayList<>();

        if (StringUtils.isBlank(keyword)) {
            return result;
        }

        // 搜索攻略建议
        LambdaQueryWrapper<TravelGuide> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TravelGuide::getReviewStatus, ContentReviewService.STATUS_APPROVED);
        queryWrapper.and(wrapper -> wrapper
            .like(TravelGuide::getTitle, keyword)
            .or()
            .like(TravelGuide::getContent, keyword)
        );
        queryWrapper.orderByDesc(TravelGuide::getCreateTime);
        queryWrapper.last("LIMIT " + limit);

        List<TravelGuide> guides = travelGuideMapper.selectList(queryWrapper);

        // 获取用户信息
        List<Long> userIds = guides.stream().map(TravelGuide::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        for (TravelGuide guide : guides) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("id", guide.getId());
            suggestion.put("title", guide.getTitle());
            suggestion.put("coverImage", guide.getCoverImage());
            suggestion.put("views", guide.getViews());
            suggestion.put("createTime", guide.getCreateTime());
            suggestion.put("destination", guide.getDestination());
            suggestion.put("type", "guide");

            // 添加用户信息
            User user = userMap.get(guide.getUserId());
            if (user != null) {
                suggestion.put("userNickname", user.getNickname());
                suggestion.put("userAvatar", user.getAvatar());
                String roleCode = RolePermission.normalizeRole(user.getRoleCode());
                suggestion.put("userRoleCode", roleCode);
                suggestion.put("userRoleName", RolePermission.roleNameOf(roleCode));
            }

            result.add(suggestion);
        }

        return result;
    }
}
