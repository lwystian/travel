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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_PREFIX = "auth:sms:code:";
    private static final String SENT_PREFIX = "auth:sms:sent:";
    private static final String DAILY_PREFIX = "auth:sms:daily:";
    private static final String SALT = "travel-auth-sms";

    private final Map<String, MemoryCode> memoryCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> memorySentAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> memoryDaily = new ConcurrentHashMap<>();

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private AuthConfigService authConfigService;

    @Resource
    private AliyunSmsSenderService aliyunSmsSenderService;

    public void sendCode(String phone, String scene) {
        validatePhone(phone);
        String normalizedScene = normalizeScene(scene);
        AliyunSmsConfigDTO config = authConfigService.getSmsConfigForSend();
        if (!Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("短信服务未完成配置，请联系管理员");
        }
        checkRateLimit(phone, normalizedScene, config);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        aliyunSmsSenderService.sendCode(config, phone, code);
        storeCode(phone, normalizedScene, code, config.getCodeExpireMinutes() * 60L);
        markSent(phone, normalizedScene, config);
    }

    public void verifyCode(String phone, String scene, String code) {
        validatePhone(phone);
        if (!StringUtils.hasText(code) || !code.matches("\\d{6}")) {
            throw new ServiceException("验证码格式不正确");
        }

        String normalizedScene = normalizeScene(scene);
        String key = codeKey(phone, normalizedScene);
        String expected = readStringFromRedis(key);
        if (!StringUtils.hasText(expected)) {
            MemoryCode memoryCode = memoryCodes.get(key);
            if (memoryCode != null && memoryCode.expireAt >= System.currentTimeMillis()) {
                expected = memoryCode.codeHash;
            }
        }

        if (!StringUtils.hasText(expected)) {
            throw new ServiceException("验证码已过期，请重新获取");
        }
        if (!expected.equals(hashCode(phone, normalizedScene, code))) {
            throw new ServiceException("验证码错误");
        }

        deleteRedis(key);
        memoryCodes.remove(key);
    }

    public String normalizeScene(String scene) {
        String normalized = StringUtils.hasText(scene) ? scene.trim().toUpperCase(Locale.ROOT) : "";
        if (!"REGISTER".equals(normalized) && !"LOGIN".equals(normalized)) {
            throw new ServiceException("验证码场景不正确");
        }
        return normalized;
    }

    public void validatePhone(String phone) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("请输入正确的手机号");
        }
    }

    private void checkRateLimit(String phone, String scene, AliyunSmsConfigDTO config) {
        String sentKey = SENT_PREFIX + scene + ":" + phone;
        if (readStringFromRedis(sentKey) != null || memorySentAt.getOrDefault(sentKey, 0L) > System.currentTimeMillis()) {
            throw new ServiceException(config.getSendIntervalSeconds() + "秒内请勿重复获取验证码");
        }

        String dailyKey = DAILY_PREFIX + scene + ":" + phone + ":" + LocalDate.now();
        Integer dailyCount = readIntegerFromRedis(dailyKey);
        if (dailyCount == null) {
            dailyCount = memoryDaily.getOrDefault(dailyKey, 0);
        }
        if (dailyCount >= config.getDailyLimit()) {
            throw new ServiceException("今日验证码获取次数已达上限，请明天再试");
        }
    }

    private void storeCode(String phone, String scene, String code, long ttlSeconds) {
        String key = codeKey(phone, scene);
        String hash = hashCode(phone, scene, code);
        if (!setRedis(key, hash, ttlSeconds)) {
            memoryCodes.put(key, new MemoryCode(hash, System.currentTimeMillis() + ttlSeconds * 1000));
        }
    }

    private void markSent(String phone, String scene, AliyunSmsConfigDTO config) {
        String sentKey = SENT_PREFIX + scene + ":" + phone;
        if (!setRedis(sentKey, "1", config.getSendIntervalSeconds())) {
            memorySentAt.put(sentKey, System.currentTimeMillis() + config.getSendIntervalSeconds() * 1000L);
        }

        String dailyKey = DAILY_PREFIX + scene + ":" + phone + ":" + LocalDate.now();
        try {
            long count = redisUtil.incr(dailyKey, 1);
            if (count == 1) {
                redisUtil.expire(dailyKey, 24 * 60 * 60);
            }
        } catch (Exception e) {
            memoryDaily.merge(dailyKey, 1, Integer::sum);
        }
    }

    private String codeKey(String phone, String scene) {
        return CODE_PREFIX + scene + ":" + phone;
    }

    private String hashCode(String phone, String scene, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT + ":" + scene + ":" + phone + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
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

    private void deleteRedis(String key) {
        try {
            redisUtil.del(key);
        } catch (Exception ignored) {
        }
    }

    private record MemoryCode(String codeHash, long expireAt) {
    }
}
