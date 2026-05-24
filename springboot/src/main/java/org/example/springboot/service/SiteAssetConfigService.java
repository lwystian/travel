package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.dto.SiteAssetConfigDTO;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import org.example.springboot.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class SiteAssetConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SiteAssetConfigService.class);
    private static final String CONFIG_TYPE = "site_assets";
    private static final String CONFIG_NAME = "site asset config";
    private static final String CONFIG_DESCRIPTION = "Configurable site images used by the frontend template";

    public SiteAssetConfigDTO getPublicConfig() {
        AuthProviderConfig entity = getOrCreate();
        SiteAssetConfigDTO dto = parse(entity.getConfigData());
        normalize(dto);
        return dto;
    }

    public SiteAssetConfigDTO getAdminConfig() {
        return getPublicConfig();
    }

    @Transactional
    public void saveConfig(SiteAssetConfigDTO dto) {
        if (dto == null) {
            throw new ServiceException("site asset config cannot be empty");
        }
        normalize(dto);
        AuthProviderConfig entity = getOrCreate();
        SiteAssetConfigDTO oldConfig = parse(entity.getConfigData());
        normalize(oldConfig);
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(Boolean.TRUE);
        entity.setConfigData(JSON.toJSONString(dto));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setUpdatedAt(LocalDateTime.now());
        if (!updateById(entity)) {
            throw new ServiceException("failed to save site asset config");
        }
        deleteReplacedLocalImages(oldConfig, dto);
    }

    private void normalize(SiteAssetConfigDTO dto) {
        dto.setFaviconUrl(normalizeImagePath(dto.getFaviconUrl()));
        dto.setLogoUrl(normalizeImagePath(dto.getLogoUrl()));
        dto.setWechatQrUrl(normalizeImagePath(dto.getWechatQrUrl()));
        dto.setAuthBackgroundUrl(normalizeImagePath(dto.getAuthBackgroundUrl()));
        dto.setAboutHeroUrl(normalizeImagePath(dto.getAboutHeroUrl()));
        dto.setLegalHeroUrl(normalizeImagePath(dto.getLegalHeroUrl()));
        dto.setAccommodationHeroUrl(normalizeImagePath(dto.getAccommodationHeroUrl()));
        dto.setPlaceholderImageUrl(normalizeImagePath(dto.getPlaceholderImageUrl()));
    }

    private String normalizeImagePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim();
        cleaned = cleaned.length() > 300 ? cleaned.substring(0, 300) : cleaned;
        if (cleaned.startsWith("/") || cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return cleaned;
        }
        return "";
    }

    private void deleteReplacedLocalImages(SiteAssetConfigDTO oldConfig, SiteAssetConfigDTO newConfig) {
        Set<String> newImages = collectImagePaths(newConfig);
        for (String oldImage : collectImagePaths(oldConfig)) {
            if (!newImages.contains(oldImage) && isLocalImagePath(oldImage)) {
                boolean deleted = FileUtil.deleteFile(oldImage);
                if (!deleted) {
                    LOGGER.warn("Failed to delete replaced site asset image: {}", oldImage);
                }
            }
        }
    }

    private Set<String> collectImagePaths(SiteAssetConfigDTO dto) {
        Set<String> images = new LinkedHashSet<>();
        if (dto == null) {
            return images;
        }
        addImagePath(images, dto.getFaviconUrl());
        addImagePath(images, dto.getLogoUrl());
        addImagePath(images, dto.getWechatQrUrl());
        addImagePath(images, dto.getAuthBackgroundUrl());
        addImagePath(images, dto.getAboutHeroUrl());
        addImagePath(images, dto.getLegalHeroUrl());
        addImagePath(images, dto.getAccommodationHeroUrl());
        addImagePath(images, dto.getPlaceholderImageUrl());
        return images;
    }

    private void addImagePath(Set<String> images, String value) {
        if (StringUtils.hasText(value)) {
            images.add(value.trim());
        }
    }

    private boolean isLocalImagePath(String value) {
        return StringUtils.hasText(value) && value.trim().startsWith("/img/");
    }

    private SiteAssetConfigDTO parse(String json) {
        if (!StringUtils.hasText(json)) {
            return new SiteAssetConfigDTO();
        }
        try {
            SiteAssetConfigDTO parsed = JSON.parseObject(json, SiteAssetConfigDTO.class);
            return parsed == null ? new SiteAssetConfigDTO() : parsed;
        } catch (Exception ignored) {
            return new SiteAssetConfigDTO();
        }
    }

    private AuthProviderConfig getOrCreate() {
        AuthProviderConfig entity = getOne(new LambdaQueryWrapper<AuthProviderConfig>()
                .eq(AuthProviderConfig::getConfigType, CONFIG_TYPE));
        if (entity != null) {
            return entity;
        }
        entity = new AuthProviderConfig();
        entity.setConfigType(CONFIG_TYPE);
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(Boolean.TRUE);
        entity.setConfigData(JSON.toJSONString(new SiteAssetConfigDTO()));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (!save(entity)) {
            throw new ServiceException("failed to initialize site asset config");
        }
        return entity;
    }
}
