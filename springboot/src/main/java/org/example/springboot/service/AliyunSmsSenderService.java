package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class AliyunSmsSenderService {
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendCode(AliyunSmsConfigDTO config, String phone, String code) {
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("短信服务未启用或未完成配置，请联系管理员");
        }

        try {
            Map<String, String> params = new TreeMap<>();
            params.put("Action", "SendSms");
            params.put("Version", "2017-05-25");
            params.put("RegionId", config.getRegionId());
            params.put("PhoneNumbers", phone);
            params.put("SignName", config.getSignName());
            params.put("TemplateCode", config.getTemplateCode());
            params.put("TemplateParam", JSON.toJSONString(Map.of("code", code)));
            params.put("Format", "JSON");
            params.put("AccessKeyId", config.getAccessKeyId());
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("SignatureVersion", "1.0");
            params.put("Timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC)));

            String canonicalizedQuery = buildCanonicalizedQuery(params);
            String stringToSign = "GET&%2F&" + percentEncode(canonicalizedQuery);
            String signature = sign(stringToSign, config.getAccessKeySecret() + "&");
            String url = "https://" + config.getEndpoint() + "/?Signature=" + percentEncode(signature) + "&" + canonicalizedQuery;

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);
            String resultCode = json == null ? null : json.getString("Code");
            if (!"OK".equalsIgnoreCase(resultCode)) {
                String message = json == null ? "短信服务无响应" : json.getString("Message");
                throw new ServiceException("短信发送失败：" + (StringUtils.hasText(message) ? message : resultCode));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("短信发送失败，请稍后再试");
        }
    }

    private String buildCanonicalizedQuery(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append("&");
            }
            builder.append(percentEncode(entry.getKey())).append("=").append(percentEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private String sign(String source, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
    }

    private String percentEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
