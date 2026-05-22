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
        dto.setClosedTitle(clean(dto.getClosedTitle(), "网站维护中", 40));
        dto.setClosedMessage(clean(dto.getClosedMessage(), "我们正在进行系统维护与服务升级，完成后将第一时间恢复访问。", 160));
        dto.setClosedContact(clean(dto.getClosedContact(), "如有紧急订单或出行问题，请联系官方客服处理。", 120));
        dto.setMobileTitle(clean(dto.getMobileTitle(), "请使用电脑访问", 40));
        dto.setMobileMessage(clean(dto.getMobileMessage(), "当前官网桌面版正在服务中，移动端 H5 模板正在制作，为保证浏览和下单体验，请使用电脑访问。", 180));
        dto.setMobileContact(clean(dto.getMobileContact(), "如需咨询行程，可通过官方客服渠道联系我们。", 120));
        dto.setSupportButtonText(clean(dto.getSupportButtonText(), "联系官方客服", 30));
        dto.setSupportUrl(cleanNullable(dto.getSupportUrl(), 300));
        dto.setSupportCredential(cleanNullable(dto.getSupportCredential(), 120));
        dto.setSupportQrImageUrl(cleanNullable(dto.getSupportQrImageUrl(), 300));
    }

    private String clean(String value, String fallback, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            text = fallback;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
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
