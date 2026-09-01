package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.springboot.dto.TourProductSourceConfigDTO;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TourProductSourceConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    public static final String SOURCE_LOCAL = "LOCAL";
    public static final String SOURCE_MINIAPP = "MINIAPP";

    private static final String CONFIG_TYPE = "tour_product_source";
    private static final String CONFIG_NAME = "行程商品来源";
    private static final String CONFIG_DESCRIPTION = "控制官网前台使用本地商品或小程序统一商品";

    @Value("${app.tour-source.default-miniapp-api-base-url:http://127.0.0.1:8080/api}")
    private String defaultMiniappApiBaseUrl;

    @Value("${app.tour-source.default-miniapp-booking-url-template:}")
    private String defaultMiniappBookingUrlTemplate;

    @Value("${app.tour-source.allowed-api-hosts:}")
    private String allowedApiHosts;

    public TourProductSourceConfigDTO getConfig() {
        AuthProviderConfig entity = getOrCreate();
        TourProductSourceConfigDTO dto = parse(entity.getConfigData());
        normalize(dto, false);
        return dto;
    }

    public boolean isMiniappMode() {
        return SOURCE_MINIAPP.equals(getConfig().getSourceMode());
    }

    public boolean shouldFallbackToLocal() {
        return Boolean.TRUE.equals(getConfig().getFallbackToLocal());
    }

    @Transactional
    public void saveConfig(TourProductSourceConfigDTO dto) {
        if (dto == null) {
            throw new ServiceException("商品来源配置不能为空");
        }
        normalize(dto, true);
        AuthProviderConfig entity = getOrCreate();
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(SOURCE_MINIAPP.equals(dto.getSourceMode()));
        entity.setConfigData(JSON.toJSONString(dto));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setUpdatedAt(LocalDateTime.now());
        if (!updateById(entity)) {
            throw new ServiceException("保存商品来源配置失败");
        }
    }

    public TourProductSourceConfigDTO publicView() {
        TourProductSourceConfigDTO source = getConfig();
        TourProductSourceConfigDTO dto = new TourProductSourceConfigDTO();
        dto.setSourceMode(source.getSourceMode());
        dto.setFallbackToLocal(source.getFallbackToLocal());
        dto.setMiniappApiBaseUrl("");
        dto.setMiniappBookingUrlTemplate("");
        return dto;
    }

    public TourProductSourceConfigDTO prepareForConnectionCheck(TourProductSourceConfigDTO source) {
        if (source == null) return getConfig();
        TourProductSourceConfigDTO dto = new TourProductSourceConfigDTO();
        dto.setSourceMode(source.getSourceMode());
        dto.setFallbackToLocal(source.getFallbackToLocal());
        dto.setMiniappApiBaseUrl(source.getMiniappApiBaseUrl());
        dto.setMiniappBookingUrlTemplate(source.getMiniappBookingUrlTemplate());
        normalize(dto, false);
        return dto;
    }

    private void normalize(TourProductSourceConfigDTO dto, boolean validateEnabledMode) {
        String mode = clean(dto.getSourceMode(), 20).toUpperCase();
        dto.setSourceMode(SOURCE_MINIAPP.equals(mode) ? SOURCE_MINIAPP : SOURCE_LOCAL);
        dto.setMiniappApiBaseUrl(trimTrailingSlash(defaultIfBlank(
                clean(dto.getMiniappApiBaseUrl(), 500),
                clean(defaultMiniappApiBaseUrl, 500)
        )));
        dto.setMiniappBookingUrlTemplate(defaultIfBlank(
                clean(dto.getMiniappBookingUrlTemplate(), 1000),
                clean(defaultMiniappBookingUrlTemplate, 1000)
        ));
        if (dto.getFallbackToLocal() == null) {
            dto.setFallbackToLocal(true);
        }
        if (validateEnabledMode && SOURCE_MINIAPP.equals(dto.getSourceMode())) {
            validateApiBaseUrl(dto.getMiniappApiBaseUrl());
        } else if (!dto.getMiniappApiBaseUrl().isBlank()) {
            validateApiBaseUrl(dto.getMiniappApiBaseUrl());
        }
    }

    private void validateHttpUrl(String value, String label) {
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ignored) {
            throw new ServiceException(label + "必须是完整的 http 或 https 地址");
        }
    }

    private void validateApiBaseUrl(String value) {
        validateHttpUrl(value, "小程序 API 地址");
        URI uri = URI.create(value);
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new ServiceException("小程序 API 地址不能包含查询参数或锚点");
        }
        Set<String> allowlist = Arrays.stream(defaultIfBlank(allowedApiHosts, "").split(","))
                .map(String::trim)
                .filter(host -> !host.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (!allowlist.isEmpty() && !allowlist.contains(uri.getHost().toLowerCase())) {
            throw new ServiceException("小程序 API 域名不在服务器允许列表中");
        }
    }

    private TourProductSourceConfigDTO parse(String json) {
        try {
            TourProductSourceConfigDTO parsed = JSON.parseObject(json, TourProductSourceConfigDTO.class);
            return parsed == null ? new TourProductSourceConfigDTO() : parsed;
        } catch (Exception ignored) {
            return new TourProductSourceConfigDTO();
        }
    }

    private synchronized AuthProviderConfig getOrCreate() {
        AuthProviderConfig entity = getOne(new LambdaQueryWrapper<AuthProviderConfig>()
                .eq(AuthProviderConfig::getConfigType, CONFIG_TYPE));
        if (entity != null) {
            return entity;
        }
        TourProductSourceConfigDTO defaults = new TourProductSourceConfigDTO();
        defaults.setMiniappApiBaseUrl(clean(defaultMiniappApiBaseUrl, 500));
        defaults.setMiniappBookingUrlTemplate(clean(defaultMiniappBookingUrlTemplate, 1000));
        entity = new AuthProviderConfig();
        entity.setConfigType(CONFIG_TYPE);
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(Boolean.FALSE);
        entity.setConfigData(JSON.toJSONString(defaults));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (!save(entity)) {
            throw new ServiceException("初始化商品来源配置失败");
        }
        return entity;
    }

    private String clean(String value, int maxLength) {
        String text = value == null ? "" : value.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? (fallback == null ? "" : fallback.trim()) : value;
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }
}
