package org.example.springboot.service;

import org.example.springboot.dto.GeetestConfigDTO;
import org.example.springboot.dto.GeetestVerifyDTO;
import org.example.springboot.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeetestCaptchaService {
    private static final String GEETEST_REGISTER_URL = "https://api.geetest.com/register.php?gt=";

    private final RestTemplate restTemplate = new RestTemplate();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Boolean> issuedChallenges = new ConcurrentHashMap<>();

    public GeetestConfigDTO register(GeetestConfigDTO config) {
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            GeetestConfigDTO publicConfig = new GeetestConfigDTO();
            publicConfig.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
            publicConfig.setConfigured(false);
            publicConfig.setCaptchaId(config.getCaptchaId());
            publicConfig.setCaptchaKey(null);
            return publicConfig;
        }

        GeetestConfigDTO publicConfig = new GeetestConfigDTO();
        publicConfig.setEnabled(config.getEnabled());
        publicConfig.setConfigured(config.getConfigured());
        publicConfig.setCaptchaId(config.getCaptchaId());
        publicConfig.setCaptchaKey(null);

        try {
            String response = restTemplate.getForObject(GEETEST_REGISTER_URL + encode(config.getCaptchaId()), String.class);
            String challengeSeed = StringUtils.hasText(response) ? response.trim() : "";
            if (challengeSeed.length() == 32) {
                publicConfig.setChallenge(md5(challengeSeed + config.getCaptchaKey()));
                publicConfig.setSuccess(true);
                issuedChallenges.put(publicConfig.getChallenge(), true);
                return publicConfig;
            }
        } catch (Exception ignored) {
            // Fall back to Geetest v3 offline mode when the register API is temporarily unavailable.
        }

        publicConfig.setChallenge(md5(randomChallenge()));
        publicConfig.setSuccess(false);
        issuedChallenges.put(publicConfig.getChallenge(), false);
        return publicConfig;
    }

    public void verify(GeetestConfigDTO config, GeetestVerifyDTO dto) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        if (!Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("极验验证未启用或未完成配置，请联系管理员");
        }
        if (dto == null
                || !StringUtils.hasText(dto.getGeetestChallenge())
                || !StringUtils.hasText(dto.getGeetestValidate())
                || !StringUtils.hasText(dto.getGeetestSeccode())) {
            throw new ServiceException("请先完成滑动拼图验证");
        }

        try {
            String challenge = dto.getGeetestChallenge();
            String validate = dto.getGeetestValidate();
            Boolean issuedOnline = issuedChallenges.remove(challenge);

            boolean onlineValidated = md5(config.getCaptchaKey() + "geetest" + challenge).equalsIgnoreCase(validate);
            boolean offlineValidated = Boolean.FALSE.equals(issuedOnline) && md5(challenge).equalsIgnoreCase(validate);
            if (!onlineValidated && !offlineValidated) {
                throw new ServiceException("滑动拼图验证失败，请重新验证");
            }

            return;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("滑动拼图验证失败，请稍后重试");
        }
    }

    private String md5(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ServiceException("滑动拼图验证失败，请稍后重试");
        }
    }

    private String randomChallenge() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
