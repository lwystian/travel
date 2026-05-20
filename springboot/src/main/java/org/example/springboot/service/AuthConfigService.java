package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.dto.EmailSmtpConfigDTO;
import org.example.springboot.dto.GeetestConfigDTO;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    public static final String TYPE_ALIYUN_SMS = "aliyun_sms";
    public static final String TYPE_GEETEST = "geetest";
    public static final String TYPE_EMAIL_SMTP = "email_smtp";

    public AliyunSmsConfigDTO getSmsConfigForAdmin() {
        AliyunSmsConfigDTO config = getSmsConfig(false);
        if (StringUtils.hasText(config.getAccessKeySecret())) {
            config.setConfigured(true);
            config.setAccessKeySecret("");
        }
        return config;
    }

    public AliyunSmsConfigDTO getSmsConfigForSend() {
        return getSmsConfig(true);
    }

    public GeetestConfigDTO getGeetestConfigForAdmin() {
        GeetestConfigDTO config = getGeetestConfig(true);
        if (StringUtils.hasText(config.getCaptchaKey())) {
            config.setConfigured(true);
            config.setCaptchaKey("");
        }
        return config;
    }

    public GeetestConfigDTO getGeetestConfigForVerify() {
        return getGeetestConfig(true);
    }

    public EmailSmtpConfigDTO getEmailConfigForAdmin() {
        EmailSmtpConfigDTO config = getEmailConfig(true);
        if (StringUtils.hasText(config.getPassword())) {
            config.setConfigured(true);
            config.setPassword("");
        }
        return config;
    }

    public EmailSmtpConfigDTO getEmailConfigForSend() {
        return getEmailConfig(true);
    }

    public GeetestConfigDTO getGeetestPublicConfig() {
        GeetestConfigDTO config = getGeetestConfig(false);
        config.setCaptchaKey(null);
        return config;
    }

    @Transactional
    public void saveSmsConfig(AliyunSmsConfigDTO dto) {
        AliyunSmsConfigDTO oldConfig = getSmsConfig(true);
        if (!StringUtils.hasText(dto.getAccessKeySecret())) {
            dto.setAccessKeySecret(oldConfig.getAccessKeySecret());
        }
        fillSmsDefaults(dto);
        saveConfig(TYPE_ALIYUN_SMS, "阿里云短信", Boolean.TRUE.equals(dto.getEnabled()), JSON.toJSONString(dto), "用于登录注册短信验证码发送");
    }

    @Transactional
    public void saveGeetestConfig(GeetestConfigDTO dto) {
        GeetestConfigDTO oldConfig = getGeetestConfig(true);
        if (!StringUtils.hasText(dto.getCaptchaKey())) {
            dto.setCaptchaKey(oldConfig.getCaptchaKey());
        }
        saveConfig(TYPE_GEETEST, "极验验证", Boolean.TRUE.equals(dto.getEnabled()), JSON.toJSONString(dto), "用于短信发送前的人机验证");
    }

    @Transactional
    public void saveEmailConfig(EmailSmtpConfigDTO dto) {
        EmailSmtpConfigDTO oldConfig = getEmailConfig(true);
        if (!StringUtils.hasText(dto.getPassword())) {
            dto.setPassword(oldConfig.getPassword());
        }
        fillEmailDefaults(dto);
        saveConfig(TYPE_EMAIL_SMTP, "SMTP邮件", Boolean.TRUE.equals(dto.getEnabled()), JSON.toJSONString(dto), "用于邮箱绑定和邮件验证码发送");
    }

    private AliyunSmsConfigDTO getSmsConfig(boolean includeSecret) {
        AuthProviderConfig entity = getOrCreate(TYPE_ALIYUN_SMS, "阿里云短信", "用于登录注册短信验证码发送");
        AliyunSmsConfigDTO dto = parse(entity.getConfigData(), AliyunSmsConfigDTO.class, new AliyunSmsConfigDTO());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        fillSmsDefaults(dto);
        dto.setConfigured(StringUtils.hasText(dto.getAccessKeyId())
                && StringUtils.hasText(dto.getAccessKeySecret())
                && StringUtils.hasText(dto.getSignName())
                && StringUtils.hasText(dto.getTemplateCode()));
        if (!includeSecret) {
            dto.setAccessKeySecret(null);
        }
        return dto;
    }

    private GeetestConfigDTO getGeetestConfig(boolean includeSecret) {
        AuthProviderConfig entity = getOrCreate(TYPE_GEETEST, "极验验证", "用于短信发送前的人机验证");
        GeetestConfigDTO dto = parse(entity.getConfigData(), GeetestConfigDTO.class, new GeetestConfigDTO());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        dto.setConfigured(StringUtils.hasText(dto.getCaptchaId()) && StringUtils.hasText(dto.getCaptchaKey()));
        if (!includeSecret) {
            dto.setCaptchaKey(null);
        }
        return dto;
    }

    private EmailSmtpConfigDTO getEmailConfig(boolean includeSecret) {
        AuthProviderConfig entity = getOrCreate(TYPE_EMAIL_SMTP, "SMTP邮件", "用于邮箱绑定和邮件验证码发送");
        EmailSmtpConfigDTO dto = parse(entity.getConfigData(), EmailSmtpConfigDTO.class, new EmailSmtpConfigDTO());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        fillEmailDefaults(dto);
        dto.setConfigured(StringUtils.hasText(dto.getHost())
                && dto.getPort() != null
                && StringUtils.hasText(dto.getUsername())
                && StringUtils.hasText(dto.getPassword())
                && StringUtils.hasText(dto.getFromEmail()));
        if (!includeSecret) {
            dto.setPassword(null);
        }
        return dto;
    }

    private void fillSmsDefaults(AliyunSmsConfigDTO dto) {
        if (!StringUtils.hasText(dto.getRegionId())) {
            dto.setRegionId("cn-hangzhou");
        }
        if (!StringUtils.hasText(dto.getEndpoint())) {
            dto.setEndpoint("dysmsapi.aliyuncs.com");
        }
        if (dto.getCodeExpireMinutes() == null || dto.getCodeExpireMinutes() < 1) {
            dto.setCodeExpireMinutes(5);
        }
        if (dto.getSendIntervalSeconds() == null || dto.getSendIntervalSeconds() < 30) {
            dto.setSendIntervalSeconds(60);
        }
        if (dto.getDailyLimit() == null || dto.getDailyLimit() < 1) {
            dto.setDailyLimit(10);
        }
    }

    private void fillEmailDefaults(EmailSmtpConfigDTO dto) {
        if (!StringUtils.hasText(dto.getHost())) {
            dto.setHost("smtp.qq.com");
        }
        if (dto.getPort() == null || dto.getPort() < 1) {
            dto.setPort(465);
        }
        if (!StringUtils.hasText(dto.getProtocol())) {
            dto.setProtocol(Boolean.TRUE.equals(dto.getSslEnabled()) ? "smtps" : "smtp");
        }
        if (dto.getSslEnabled() == null) {
            dto.setSslEnabled(true);
        }
        if (dto.getCodeExpireMinutes() == null || dto.getCodeExpireMinutes() < 1) {
            dto.setCodeExpireMinutes(10);
        }
        if (dto.getSendIntervalSeconds() == null || dto.getSendIntervalSeconds() < 30) {
            dto.setSendIntervalSeconds(60);
        }
    }

    private <T> T parse(String json, Class<T> clazz, T fallback) {
        if (!StringUtils.hasText(json)) {
            return fallback;
        }
        try {
            T parsed = JSON.parseObject(json, clazz);
            return parsed == null ? fallback : parsed;
        } catch (Exception e) {
            return fallback;
        }
    }

    private AuthProviderConfig getOrCreate(String type, String name, String description) {
        AuthProviderConfig entity = getOne(new LambdaQueryWrapper<AuthProviderConfig>()
                .eq(AuthProviderConfig::getConfigType, type));
        if (entity != null) {
            return entity;
        }
        entity = new AuthProviderConfig();
        entity.setConfigType(type);
        entity.setConfigName(name);
        entity.setEnabled(false);
        entity.setConfigData("{}");
        entity.setDescription(description);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);
        return entity;
    }

    private void saveConfig(String type, String name, boolean enabled, String data, String description) {
        AuthProviderConfig entity = getOrCreate(type, name, description);
        entity.setConfigName(name);
        entity.setEnabled(enabled);
        entity.setConfigData(data);
        entity.setDescription(description);
        entity.setUpdatedAt(LocalDateTime.now());
        if (!updateById(entity)) {
            throw new ServiceException("保存认证配置失败");
        }
    }
}
