package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import org.example.springboot.dto.AliyunSmsConfigDTO;
import org.example.springboot.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class AliyunSmsSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunSmsSenderService.class);

    public void sendCode(AliyunSmsConfigDTO config, String phone, String code) {
        if (!Boolean.TRUE.equals(config.getEnabled()) || !Boolean.TRUE.equals(config.getConfigured())) {
            throw new ServiceException("短信服务未启用或未完成配置，请联系管理员");
        }

        try {
            SendSmsResponse response = createClient(config).sendSms(new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(config.getSignName())
                    .setTemplateCode(config.getTemplateCode())
                    .setTemplateParam(JSON.toJSONString(Map.of("code", code))));

            SendSmsResponseBody body = response == null ? null : response.getBody();
            String resultCode = body == null ? null : body.getCode();
            if (!"OK".equalsIgnoreCase(resultCode)) {
                String message = body == null ? "阿里云短信服务无响应" : body.getMessage();
                throw new ServiceException("短信发送失败：" + (StringUtils.hasText(message) ? message : resultCode));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Aliyun SMS SDK request failed. endpoint={}, regionId={}, signName={}, templateCode={}, phone={}",
                    config.getEndpoint(), config.getRegionId(), config.getSignName(), config.getTemplateCode(), maskPhone(phone), e);
            throw new ServiceException("短信发送失败：" + resolveErrorMessage(e), e);
        }
    }

    private Client createClient(AliyunSmsConfigDTO config) throws Exception {
        Config sdkConfig = new Config()
                .setAccessKeyId(config.getAccessKeyId())
                .setAccessKeySecret(config.getAccessKeySecret());
        sdkConfig.endpoint = normalizeEndpoint(config.getEndpoint());
        return new Client(sdkConfig);
    }

    private String normalizeEndpoint(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return "dysmsapi.aliyuncs.com";
        }
        return endpoint.trim()
                .replaceFirst("^https?://", "")
                .replaceFirst("/+$", "");
    }

    private String resolveErrorMessage(Exception e) {
        return StringUtils.hasText(e.getMessage()) ? e.getMessage() : "阿里云短信服务调用异常";
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
