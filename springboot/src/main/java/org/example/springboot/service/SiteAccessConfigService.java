package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.dto.SiteAccessConfigDTO;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SiteAccessConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    private static final String CONFIG_TYPE = "site_access";
    private static final String CONFIG_NAME = "网站访问控制";
    private static final String CONFIG_DESCRIPTION = "控制官网前台开放状态和移动端访问策略";

    public SiteAccessConfigDTO getPublicConfig() {
        AuthProviderConfig entity = getOrCreate();
        SiteAccessConfigDTO dto = parse(entity.getConfigData());
        normalize(dto);
        return dto;
    }

    public SiteAccessConfigDTO getAdminConfig() {
        return getPublicConfig();
    }

    @Transactional
    public void saveConfig(SiteAccessConfigDTO dto) {
        if (dto == null) {
            throw new ServiceException("网站访问配置不能为空");
        }
        normalize(dto);
        AuthProviderConfig entity = getOrCreate();
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(Boolean.TRUE);
        entity.setConfigData(JSON.toJSONString(dto));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setUpdatedAt(LocalDateTime.now());
        if (!updateById(entity)) {
            throw new ServiceException("保存网站访问配置失败");
        }
    }

    private void normalize(SiteAccessConfigDTO dto) {
        if (dto.getSiteEnabled() == null) {
            dto.setSiteEnabled(true);
        }
        if (dto.getRejectMobile() == null) {
            dto.setRejectMobile(false);
        }
        dto.setClosedTitle(cleanNullable(dto.getClosedTitle(), 40));
        dto.setClosedMessage(cleanNullable(dto.getClosedMessage(), 160));
        dto.setClosedContact(cleanNullable(dto.getClosedContact(), 120));
        dto.setMobileTitle(cleanNullable(dto.getMobileTitle(), 40));
        dto.setMobileMessage(cleanNullable(dto.getMobileMessage(), 180));
        dto.setMobileContact(cleanNullable(dto.getMobileContact(), 120));
        dto.setSupportButtonText(cleanNullable(dto.getSupportButtonText(), 30));
        dto.setSupportUrl(cleanNullable(dto.getSupportUrl(), 300));
        dto.setSupportCredential(cleanNullable(dto.getSupportCredential(), 120));
        dto.setSupportQrImageUrl(cleanNullable(dto.getSupportQrImageUrl(), 300));
    }

    private String cleanNullable(String value, int maxLength) {
        String text = value == null ? "" : value.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private SiteAccessConfigDTO parse(String json) {
        try {
            SiteAccessConfigDTO parsed = JSON.parseObject(json, SiteAccessConfigDTO.class);
            return parsed == null ? new SiteAccessConfigDTO() : parsed;
        } catch (Exception ignored) {
            return new SiteAccessConfigDTO();
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
        entity.setConfigData(JSON.toJSONString(new SiteAccessConfigDTO()));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (!save(entity)) {
            throw new ServiceException("初始化网站访问配置失败");
        }
        return entity;
    }
}
