package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.example.springboot.dto.CustomerServiceConfigDTO;
import org.example.springboot.dto.SiteAccessConfigDTO;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerServiceConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    private static final String CONFIG_TYPE = "customer_service";
    private static final String CONFIG_NAME = "在线客服配置";
    private static final String CONFIG_DESCRIPTION = "控制官网客服悬浮入口和企业微信渠道链接";
    private static final List<String> CHANNELS = List.of(
            "home", "chongqing", "sanxia", "xisha", "train", "team", "tour", "order", "user"
    );

    @Resource
    private SiteAccessConfigService siteAccessConfigService;

    @PostConstruct
    public void initialize() {
        getOrCreate();
    }

    public CustomerServiceConfigDTO getPublicConfig() {
        AuthProviderConfig entity = getOrCreate();
        CustomerServiceConfigDTO dto = parse(entity.getConfigData());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        normalize(dto, false);
        return dto;
    }

    public CustomerServiceConfigDTO getAdminConfig() {
        return getPublicConfig();
    }

    @Transactional
    public void saveConfig(CustomerServiceConfigDTO dto) {
        if (dto == null) {
            throw new ServiceException("客服配置不能为空");
        }
        normalize(dto, true);
        if (Boolean.TRUE.equals(dto.getEnabled()) && dto.getServiceUrl().isBlank()) {
            throw new ServiceException("启用在线客服前，请填写有效的企业微信客服链接");
        }

        AuthProviderConfig entity = getOrCreate();
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        entity.setConfigData(JSON.toJSONString(dto));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setUpdatedAt(LocalDateTime.now());
        if (!updateById(entity)) {
            throw new ServiceException("保存在线客服配置失败");
        }
    }

    private void normalize(CustomerServiceConfigDTO dto, boolean rejectInvalidUrl) {
        if (dto.getEnabled() == null) {
            dto.setEnabled(false);
        }
        dto.setDisplayName(clean(dto.getDisplayName(), 30));
        if (dto.getDisplayName().isBlank()) {
            dto.setDisplayName("在线客服");
        }
        dto.setServiceUrl(normalizeWecomUrl(dto.getServiceUrl(), "默认客服链接", rejectInvalidUrl));

        Map<String, String> source = dto.getChannelUrls() == null ? Map.of() : dto.getChannelUrls();
        Map<String, String> channels = new LinkedHashMap<>();
        for (String channel : CHANNELS) {
            channels.put(channel, normalizeWecomUrl(source.get(channel), "页面客服链接", rejectInvalidUrl));
        }
        dto.setChannelUrls(channels);
    }

    private String normalizeWecomUrl(String value, String fieldName, boolean rejectInvalidUrl) {
        String url = clean(value, 500);
        if (url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            boolean valid = "https".equalsIgnoreCase(uri.getScheme())
                    && "work.weixin.qq.com".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/kfid/");
            if (valid) {
                return uri.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // 统一在下方返回清晰的配置错误。
        }
        if (rejectInvalidUrl) {
            throw new ServiceException(fieldName + "必须是 https://work.weixin.qq.com/kfid/ 开头的企业微信客服链接");
        }
        return "";
    }

    private String clean(String value, int maxLength) {
        String text = value == null ? "" : value.trim().replaceAll("[\\r\\n\\t]+", " ");
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private CustomerServiceConfigDTO parse(String json) {
        try {
            CustomerServiceConfigDTO parsed = JSON.parseObject(json, CustomerServiceConfigDTO.class);
            return parsed == null ? new CustomerServiceConfigDTO() : parsed;
        } catch (Exception ignored) {
            return new CustomerServiceConfigDTO();
        }
    }

    private AuthProviderConfig getOrCreate() {
        AuthProviderConfig entity = getOne(new LambdaQueryWrapper<AuthProviderConfig>()
                .eq(AuthProviderConfig::getConfigType, CONFIG_TYPE));
        if (entity != null) {
            return entity;
        }

        CustomerServiceConfigDTO initialConfig = initialConfig();
        entity = new AuthProviderConfig();
        entity.setConfigType(CONFIG_TYPE);
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(Boolean.TRUE.equals(initialConfig.getEnabled()));
        entity.setConfigData(JSON.toJSONString(initialConfig));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (!save(entity)) {
            throw new ServiceException("初始化在线客服配置失败");
        }
        return entity;
    }

    private CustomerServiceConfigDTO initialConfig() {
        CustomerServiceConfigDTO dto = new CustomerServiceConfigDTO();
        SiteAccessConfigDTO siteAccess = siteAccessConfigService.getPublicConfig();
        String existingUrl = normalizeWecomUrl(siteAccess.getSupportUrl(), "客服链接", false);
        if (!existingUrl.isBlank()) {
            dto.setEnabled(true);
            dto.setDisplayName(clean(siteAccess.getSupportButtonText(), 30));
            dto.setServiceUrl(existingUrl);
        }
        normalize(dto, false);
        return dto;
    }
}
