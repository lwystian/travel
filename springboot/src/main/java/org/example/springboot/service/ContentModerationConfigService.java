package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.ContentModerationConfigDTO;
import org.example.springboot.entity.ContentModerationConfig;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ContentModerationConfigMapper;
import org.example.springboot.security.RolePermission;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ContentModerationConfigService {
    public static final String ADMIN_GUIDE_REVIEW_REQUIRED = "admin_guide_review_required";
    public static final String ADMIN_COMMENT_REVIEW_REQUIRED = "admin_comment_review_required";

    @Resource
    private ContentModerationConfigMapper configMapper;

    public ContentModerationConfigDTO getConfig() {
        ContentModerationConfigDTO dto = new ContentModerationConfigDTO();
        boolean enabled = isEnabled(ADMIN_GUIDE_REVIEW_REQUIRED) || isEnabled(ADMIN_COMMENT_REVIEW_REQUIRED);
        dto.setAdminContentReviewRequired(enabled);
        dto.setAdminGuideReviewRequired(enabled);
        dto.setAdminCommentReviewRequired(enabled);
        return dto;
    }

    public void updateConfig(ContentModerationConfigDTO dto, User actor) {
        if (!RolePermission.isSuperAdmin(actor)) {
            throw new ServiceException("只有超级管理员可以配置审核策略");
        }
        if (dto == null) {
            return;
        }
        if (dto.getAdminContentReviewRequired() != null) {
            upsert(ADMIN_GUIDE_REVIEW_REQUIRED, dto.getAdminContentReviewRequired(),
                    "管理员发布或编辑攻略是否需要人工审核");
            upsert(ADMIN_COMMENT_REVIEW_REQUIRED, dto.getAdminContentReviewRequired(),
                    "管理员发布评论或住宿评价是否需要人工审核");
            return;
        }
        if (dto.getAdminGuideReviewRequired() != null) {
            upsert(ADMIN_GUIDE_REVIEW_REQUIRED, dto.getAdminGuideReviewRequired(),
                    "管理员发布或编辑攻略是否需要人工审核");
        }
        if (dto.getAdminCommentReviewRequired() != null) {
            upsert(ADMIN_COMMENT_REVIEW_REQUIRED, dto.getAdminCommentReviewRequired(),
                    "管理员发布评论或住宿评价是否需要人工审核");
        }
    }

    public boolean adminGuideReviewRequired() {
        return isEnabled(ADMIN_GUIDE_REVIEW_REQUIRED) || isEnabled(ADMIN_COMMENT_REVIEW_REQUIRED);
    }

    public boolean adminCommentReviewRequired() {
        return isEnabled(ADMIN_GUIDE_REVIEW_REQUIRED) || isEnabled(ADMIN_COMMENT_REVIEW_REQUIRED);
    }

    private boolean isEnabled(String key) {
        ContentModerationConfig config = configMapper.selectOne(new LambdaQueryWrapper<ContentModerationConfig>()
                .eq(ContentModerationConfig::getConfigKey, key)
                .last("LIMIT 1"));
        return config != null && truthy(config.getConfigValue());
    }

    private void upsert(String key, Boolean enabled, String description) {
        ContentModerationConfig existing = configMapper.selectOne(new LambdaQueryWrapper<ContentModerationConfig>()
                .eq(ContentModerationConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            existing = new ContentModerationConfig();
            existing.setConfigKey(key);
            existing.setDescription(description);
            existing.setConfigValue(Boolean.TRUE.equals(enabled) ? "1" : "0");
            existing.setUpdateTime(LocalDateTime.now());
            configMapper.insert(existing);
            return;
        }
        existing.setConfigValue(Boolean.TRUE.equals(enabled) ? "1" : "0");
        existing.setDescription(description);
        existing.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(existing);
    }

    private boolean truthy(String value) {
        if (value == null) {
            return false;
        }
        return Map.of("1", true, "true", true, "yes", true, "on", true)
                .getOrDefault(value.trim().toLowerCase(), false);
    }
}
