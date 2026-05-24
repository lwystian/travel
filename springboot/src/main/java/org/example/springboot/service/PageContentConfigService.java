package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PageContentConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    private static final String CONFIG_PREFIX = "page_content:";
    private static final int MAX_PAGE_KEY_LENGTH = 20;
    private static final int MAX_JSON_LENGTH = 60000;

    public Map<String, Object> getPublicContent(String pageKey) {
        AuthProviderConfig entity = getOrCreate(pageKey);
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            return new LinkedHashMap<>();
        }
        return parse(entity.getConfigData());
    }

    public Map<String, Object> getAdminContent(String pageKey) {
        return parse(getOrCreate(pageKey).getConfigData());
    }

    @Transactional
    public void saveContent(String pageKey, Map<String, Object> content) {
        if (content == null) {
            throw new ServiceException("页面内容不能为空");
        }
        String normalizedPageKey = normalizePageKey(pageKey);
        String json = JSON.toJSONString(content);
        if (json.length() > MAX_JSON_LENGTH) {
            throw new ServiceException("页面内容过大，请拆分后保存");
        }

        AuthProviderConfig entity = getOrCreate(normalizedPageKey);
        entity.setConfigType(configType(normalizedPageKey));
        entity.setConfigName("页面内容配置-" + normalizedPageKey);
        entity.setEnabled(true);
        entity.setConfigData(json);
        entity.setDescription("前台页面原位编辑内容，用于覆盖默认模板文案");
        entity.setUpdatedAt(LocalDateTime.now());
        if (!updateById(entity)) {
            throw new ServiceException("保存页面内容失败");
        }
    }

    private AuthProviderConfig getOrCreate(String pageKey) {
        String normalizedPageKey = normalizePageKey(pageKey);
        String configType = configType(normalizedPageKey);
        AuthProviderConfig entity = getOne(new LambdaQueryWrapper<AuthProviderConfig>()
                .eq(AuthProviderConfig::getConfigType, configType)
                .last("LIMIT 1"));
        if (entity != null) {
            return entity;
        }

        entity = new AuthProviderConfig();
        entity.setConfigType(configType);
        entity.setConfigName("页面内容配置-" + normalizedPageKey);
        entity.setEnabled(true);
        entity.setConfigData("{}");
        entity.setDescription("前台页面原位编辑内容，用于覆盖默认模板文案");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (!save(entity)) {
            throw new ServiceException("初始化页面内容配置失败");
        }
        return entity;
    }

    private Map<String, Object> parse(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON.parseObject(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String normalizePageKey(String pageKey) {
        if (!StringUtils.hasText(pageKey)) {
            throw new ServiceException("页面标识不能为空");
        }
        String normalized = pageKey.trim().toLowerCase();
        if (normalized.length() > MAX_PAGE_KEY_LENGTH || !normalized.matches("[a-z0-9_-]+")) {
            throw new ServiceException("页面标识不合法");
        }
        return normalized;
    }

    private String configType(String pageKey) {
        return CONFIG_PREFIX + normalizePageKey(pageKey);
    }
}
