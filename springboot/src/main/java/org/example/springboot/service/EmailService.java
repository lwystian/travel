package org.example.springboot.service;

import jakarta.annotation.Resource;
import org.example.springboot.DTO.EmailMessageDTO;
import org.example.springboot.dto.EmailSmtpConfigDTO;
import org.example.springboot.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件服务类
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String COMPANY_NAME = "侠客行国际旅行社";
    private static final String COMPANY_SHORT_NAME = "侠客行国旅";
    
    private static final ConcurrentHashMap<String, CodeInfo> EMAIL_CODE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> EMAIL_SENT_AT_MAP = new ConcurrentHashMap<>();
    
    @Resource
    private AuthConfigService authConfigService;
    
    /**
     * 发送验证码邮件（同步）
     * 
     * @param email 收件人邮箱
     * @return 验证码
     */
    public String sendVerificationCodeAsync(String email) {
        return sendCodeEmail(email, "EMAIL_BIND", "【" + COMPANY_SHORT_NAME + "】绑定邮箱验证码");
    }
    
    /**
     * 发送重置密码邮件（同步）
     * 
     * @param email 收件人邮箱
     * @return 验证码
     */
    public String sendResetPasswordEmailAsync(String email) {
        return sendCodeEmail(email, "RESET_PASSWORD", "【" + COMPANY_SHORT_NAME + "】密码重置验证码");
    }
    
    /**
     * 发送通知邮件（同步）
     * 
     * @param email   收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendNotificationEmailAsync(String email, String subject, String content) {
        // 创建邮件消息DTO
        EmailMessageDTO emailMessage = EmailMessageDTO.createNotificationEmail(email, subject, content);
        
        // 直接同步发送邮件
        sendEmail(emailMessage);
        
        logger.info("发送通知邮件：{}，主题：{}", email, subject);
    }
    
    /**
     * 发送邮件（同步）
     * 
     * @param emailMessage 邮件消息对象
     */
    public void sendEmail(EmailMessageDTO emailMessage) {
        try {
            EmailSmtpConfigDTO config = authConfigService.getEmailConfigForSend();
            JavaMailSenderImpl sender = createMailSender(config);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(config.getFromEmail());
            message.setTo(emailMessage.getTo());
            message.setSubject(emailMessage.getSubject());
            message.setText(emailMessage.getContent());
            
            sender.send(message);
            
            logger.info("邮件发送成功：{}", emailMessage.getTo());
        } catch (Exception e) {
            logger.error("邮件发送失败：{}", e.getMessage(), e);
            throw new ServiceException("邮件发送失败：" + e.getMessage());
        }
    }
    
    /**
     * 验证验证码
     * 
     * @param email 邮箱
     * @param code  验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String email, String code) {
        return verifyCode(email, "EMAIL_BIND", code);
    }

    public boolean verifyCode(String email, String scene, String code) {
        String key = codeKey(email, scene);
        CodeInfo codeInfo = EMAIL_CODE_MAP.get(key);
        if (codeInfo == null || codeInfo.expireAt < Instant.now().toEpochMilli()) {
            EMAIL_CODE_MAP.remove(key);
            return false;
        }
        if (!codeInfo.code.equals(code)) {
            return false;
        }
        EMAIL_CODE_MAP.remove(key);
        return true;
    }
    
    /**
     * 生成6位随机验证码
     * 
     * @return 验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String sendCodeEmail(String email, String scene, String subject) {
        validateEmail(email);
        EmailSmtpConfigDTO config = authConfigService.getEmailConfigForSend();
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("邮件服务未启用或未完成配置，请联系管理员");
        }
        String key = codeKey(email, scene);
        Long sentUntil = EMAIL_SENT_AT_MAP.get(key);
        if (sentUntil != null && sentUntil > Instant.now().toEpochMilli()) {
            throw new ServiceException(config.getSendIntervalSeconds() + "秒内请勿重复获取邮箱验证码");
        }

        String code = generateVerificationCode();
        long expireAt = Instant.now().plusSeconds(config.getCodeExpireMinutes() * 60L).toEpochMilli();
        EMAIL_CODE_MAP.put(key, new CodeInfo(code, expireAt));
        EMAIL_SENT_AT_MAP.put(key, Instant.now().plusSeconds(config.getSendIntervalSeconds()).toEpochMilli());

        sendEmail(EmailMessageDTO.createNotificationEmail(email, subject,
                "您好，我们是" + COMPANY_NAME + "（简称：" + COMPANY_SHORT_NAME + "）。\n\n"
                        + "您的验证码为：" + code + "，" + config.getCodeExpireMinutes() + "分钟内有效。请勿向任何人泄露验证码。\n\n"
                        + "如非您本人操作，请忽略本邮件并及时检查账号安全。\n\n"
                        + COMPANY_NAME));
        logger.info("邮箱验证码已发送：{}，scene={}", email, scene);
        return code;
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

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email) || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ServiceException("请输入正确的邮箱地址");
        }
    }

    private String codeKey(String email, String scene) {
        return scene + ":" + email.trim().toLowerCase();
    }

    private record CodeInfo(String code, long expireAt) {
    }
}
