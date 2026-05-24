package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.dto.AuthConfigTestResultDTO;
import org.example.springboot.dto.EmailSmtpConfigDTO;
import org.example.springboot.dto.GeetestConfigDTO;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

@Service
public class AuthConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    public static final String TYPE_ALIYUN_SMS = "aliyun_sms";
    public static final String TYPE_GEETEST = "geetest";
    public static final String TYPE_EMAIL_SMTP = "email_smtp";
    private static final String GEETEST_REGISTER_URL = "https://api.geetest.com/register.php?gt=";
    private final RestTemplate restTemplate = new RestTemplate();

    @Resource
    private AliyunSmsSenderService aliyunSmsSenderService;

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

    public AuthConfigTestResultDTO testSmsConfig(String phone, String templateCode) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("请输入正确的测试手机号");
        }
        AliyunSmsConfigDTO config = getSmsConfig(true);
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("阿里云短信未启用或未完成 AccessKey、签名、验证码模板配置");
        }
        String selectedTemplateCode = resolveSmsTestTemplateCode(config, templateCode);
        String code = String.format("%06d", new Random().nextInt(1000000));
        aliyunSmsSenderService.sendTemplate(config, phone.trim(), selectedTemplateCode,
                buildSmsTestTemplateParams(config, selectedTemplateCode, code));
        return AuthConfigTestResultDTO.ok(
                "短信测试发送成功",
                "已通过当前阿里云短信配置向测试手机号发送模板测试短信。",
                List.of("服务已启用", "AccessKey 与 Endpoint 可调用", "签名可用", "已选择模板：" + selectedTemplateCode, "测试手机号格式有效")
        );
    }

    private String resolveSmsTestTemplateCode(AliyunSmsConfigDTO config, String templateCode) {
        if (!StringUtils.hasText(templateCode)) {
            throw new ServiceException("请选择要测试的短信模板Code");
        }
        String selected = templateCode.trim();
        if (selected.equals(config.getTemplateCode())
                || selected.equals(config.getOrderUserTemplateCode())
                || selected.equals(config.getOrderAdminTemplateCode())) {
            return selected;
        }
        throw new ServiceException("只能选择当前已配置的短信模板Code进行测试");
    }

    private Map<String, ?> buildSmsTestTemplateParams(AliyunSmsConfigDTO config, String templateCode, String code) {
        if (templateCode.equals(config.getTemplateCode())) {
            return Map.of("code", code);
        }
        return Map.of(
                "orderNo", "TEST" + System.currentTimeMillis(),
                "tourName", "TestTour",
                "departureDate", java.time.LocalDate.now().plusDays(7).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                "amount", "100",
                "contactName", "TestUser",
                "userName", "TestUser"
        );
    }

    public AuthConfigTestResultDTO testEmailConfig(String email) {
        if (!StringUtils.hasText(email) || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ServiceException("请输入正确的测试邮箱");
        }
        EmailSmtpConfigDTO config = getEmailConfig(true);
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("SMTP邮件未启用或未完成 Host、端口、账号、授权码、发件邮箱配置");
        }
        JavaMailSenderImpl sender = createMailSender(config);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(config.getFromEmail());
        message.setTo(email.trim().toLowerCase());
        message.setSubject("【侠客行国旅】SMTP邮件配置测试");
        message.setText("您好，我们是侠客行国际旅行社（简称：侠客行国旅）。\n\n"
                + "如果您收到这封邮件，说明后台SMTP邮件服务已完成连通性、账号认证和发件能力测试。\n\n"
                + "测试时间：" + LocalDateTime.now() + "\n\n"
                + "侠客行国际旅行社");
        sender.send(message);
        return AuthConfigTestResultDTO.ok(
                "邮件测试发送成功",
                "已通过当前SMTP配置向测试邮箱发送测试邮件。",
                List.of("服务已启用", "SMTP主机和端口可连接", "账号授权认证通过", "发件邮箱可发送")
        );
    }

    public AuthConfigTestResultDTO testGeetestConfig() {
        GeetestConfigDTO config = getGeetestConfig(true);
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("极验验证未启用或未完成 Captcha ID、Captcha Key 配置");
        }
        try {
            String response = restTemplate.getForObject(GEETEST_REGISTER_URL + java.net.URLEncoder.encode(config.getCaptchaId(), java.nio.charset.StandardCharsets.UTF_8), String.class);
            String challengeSeed = StringUtils.hasText(response) ? response.trim() : "";
            if (challengeSeed.length() != 32) {
                throw new ServiceException("极验服务返回异常，请检查 Captcha ID 是否正确");
            }
            return AuthConfigTestResultDTO.ok(
                    "极验测试通过",
                    "已成功连接极验服务并获取挑战参数。",
                    List.of("服务已启用", "Captcha ID 已配置", "Captcha Key 已配置", "极验注册接口可访问")
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("极验服务连通性测试失败：" + (StringUtils.hasText(e.getMessage()) ? e.getMessage() : "网络或服务异常"));
        }
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

    private JavaMailSenderImpl createMailSender(EmailSmtpConfigDTO config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setDefaultEncoding("UTF-8");
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.transport.protocol", StringUtils.hasText(config.getProtocol()) ? config.getProtocol() : "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", String.valueOf(Boolean.TRUE.equals(config.getSslEnabled())));
        properties.put("mail.smtp.starttls.enable", String.valueOf(!Boolean.TRUE.equals(config.getSslEnabled())));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return sender;
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
