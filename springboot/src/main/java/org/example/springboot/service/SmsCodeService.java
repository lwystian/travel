package org.example.springboot.service;

import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.util.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class SmsCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_PREFIX = "auth:sms:code:";
    private static final String SENT_PREFIX = "auth:sms:sent:";
    private static final String DAILY_PREFIX = "auth:sms:daily:";
    private static final String ATTEMPT_PREFIX = "auth:sms:attempt:";
    private static final String SALT = "travel-auth-sms";
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private AuthConfigService authConfigService;

    @Resource
    private AliyunSmsSenderService aliyunSmsSenderService;

    public void sendCode(String phone, String scene) {
        sendCode(phone, scene, null);
    }

    public void sendCode(String phone, String scene, String subject) {
        validatePhone(phone);
        String normalizedScene = normalizeScene(scene);
        String normalizedSubject = normalizeSubject(phone, subject);
        AliyunSmsConfigDTO config = authConfigService.getSmsConfigForSend();
        if (!Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("短信服务未完成配置，请联系管理员");
        }
        ensureVerificationStoreAvailable();
        checkRateLimit(phone, normalizedScene, normalizedSubject, config);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        aliyunSmsSenderService.sendCode(config, phone, code);
        storeCode(phone, normalizedScene, normalizedSubject, code, config.getCodeExpireMinutes() * 60L);
        markSent(phone, normalizedScene, normalizedSubject, config);
    }

    public void verifyCode(String phone, String scene, String code) {
        verifyCode(phone, scene, code, null);
    }

    public void verifyCode(String phone, String scene, String code, String subject) {
        validatePhone(phone);
        if (!StringUtils.hasText(code) || !code.matches("\\d{6}")) {
            throw new ServiceException("验证码格式不正确");
        }

        String normalizedScene = normalizeScene(scene);
        String normalizedSubject = normalizeSubject(phone, subject);
        String key = codeKey(phone, normalizedScene, normalizedSubject);
        String attemptKey = attemptKey(phone, normalizedScene, normalizedSubject);
        ensureVerifyAttemptsAvailable(attemptKey);
        String expected = readStringFromRedis(key);

        if (!StringUtils.hasText(expected)) {
            throw new ServiceException("验证码已过期，请重新获取");
        }
        if (!expected.equals(hashCode(phone, normalizedScene, normalizedSubject, code))) {
            recordVerifyFailure(attemptKey, key);
            throw new ServiceException("验证码错误");
        }

        deleteRedis(key, attemptKey);
    }

    public String normalizeScene(String scene) {
        String normalized = StringUtils.hasText(scene) ? scene.trim().toUpperCase(Locale.ROOT) : "";
        if (!"REGISTER".equals(normalized) && !"LOGIN".equals(normalized) && !"CHANGE_PHONE".equals(normalized)
                && !"VERIFY_CURRENT".equals(normalized)) {
            throw new ServiceException("验证码场景不正确");
        }
        return normalized;
    }

    public void validatePhone(String phone) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("请输入正确的手机号");
        }
    }

    private void checkRateLimit(String phone, String scene, String subject, AliyunSmsConfigDTO config) {
        String sentKey = SENT_PREFIX + scene + ":" + phone;
        if (readStringFromRedis(sentKey) != null) {
            throw new ServiceException(config.getSendIntervalSeconds() + "秒内请勿重复获取验证码");
        }

        String dailyKey = DAILY_PREFIX + scene + ":" + phone + ":" + LocalDate.now();
        Integer dailyCount = readIntegerFromRedis(dailyKey);
        if (dailyCount != null && dailyCount >= config.getDailyLimit()) {
            throw new ServiceException("今日验证码获取次数已达上限，请明天再试");
        }
    }

    private void storeCode(String phone, String scene, String subject, String code, long ttlSeconds) {
        String key = codeKey(phone, scene, subject);
        String attemptKey = attemptKey(phone, scene, subject);
        String hash = hashCode(phone, scene, subject, code);
        deleteRedis(key, attemptKey);
        if (!setRedis(key, hash, ttlSeconds)) {
            throw new ServiceException("验证码服务暂不可用，请稍后再试");
        }
    }

    private void markSent(String phone, String scene, String subject, AliyunSmsConfigDTO config) {
        String sentKey = SENT_PREFIX + scene + ":" + phone;
        if (!setRedis(sentKey, "1", config.getSendIntervalSeconds())) {
            throw new ServiceException("验证码服务暂不可用，请稍后再试");
        }

        String dailyKey = DAILY_PREFIX + scene + ":" + phone + ":" + LocalDate.now();
        try {
            long count = redisUtil.incr(dailyKey, 1);
            if (count == 1) {
                redisUtil.expire(dailyKey, 24 * 60 * 60);
            }
        } catch (Exception e) {
            throw new ServiceException("验证码服务暂不可用，请稍后再试");
        }
    }

    private void ensureVerifyAttemptsAvailable(String attemptKey) {
        Integer attempts = readIntegerFromRedis(attemptKey);
        if (attempts != null && attempts >= MAX_VERIFY_ATTEMPTS) {
            throw new ServiceException("验证码错误次数过多，请重新获取");
        }
    }

    private void recordVerifyFailure(String attemptKey, String codeKey) {
        long ttl = redisTtlSeconds(codeKey);
        if (ttl <= 0) {
            ttl = 300;
        }
        try {
            long attempts = redisUtil.incr(attemptKey, 1);
            if (attempts == 1) {
                redisUtil.expire(attemptKey, ttl);
            }
            if (attempts >= MAX_VERIFY_ATTEMPTS) {
                deleteRedis(codeKey);
                throw new ServiceException("验证码错误次数过多，请重新获取");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("验证码服务暂不可用，请稍后再试");
        }
    }

    private void ensureVerificationStoreAvailable() {
        String probeKey = CODE_PREFIX + "probe:" + System.nanoTime();
        if (!setRedis(probeKey, "1", 5)) {
            throw new ServiceException("验证码服务暂不可用，请稍后再试");
        }
        deleteRedis(probeKey);
    }

    private String codeKey(String phone, String scene, String subject) {
        return CODE_PREFIX + scene + ":" + safeKeyPart(subject) + ":" + phone;
    }

    private String attemptKey(String phone, String scene, String subject) {
        return ATTEMPT_PREFIX + scene + ":" + safeKeyPart(subject) + ":" + phone;
    }

    private String hashCode(String phone, String scene, String subject, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT + ":" + scene + ":" + subject + ":" + phone + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new ServiceException("验证码处理失败");
        }
    }

    private String normalizeSubject(String phone, String subject) {
        if (!StringUtils.hasText(subject)) {
            return "PHONE:" + phone;
        }
        return subject.trim().toUpperCase(Locale.ROOT);
    }

    private String safeKeyPart(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception e) {
            throw new ServiceException("验证码处理失败");
        }
    }

    private boolean setRedis(String key, Object value, long seconds) {
        try {
            return redisUtil.set(key, value, seconds);
        } catch (Exception e) {
            return false;
        }
    }

    private String readStringFromRedis(String key) {
        try {
            Object value = redisUtil.get(key);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer readIntegerFromRedis(String key) {
        try {
            Object value = redisUtil.get(key);
            if (value == null) {
                return null;
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private long redisTtlSeconds(String key) {
        try {
            return redisUtil.getExpire(key);
        } catch (Exception e) {
            return -1;
        }
    }

    private void deleteRedis(String... keys) {
        try {
            redisUtil.del(keys);
        } catch (Exception ignored) {
        }
    }
}
