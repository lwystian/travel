package org.example.springboot.service;

import jakarta.annotation.Resource;
import org.example.springboot.dto.SiteAccessConfigDTO;
import org.example.springboot.dto.TourProductSourceConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MiniappCustomerServiceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiniappCustomerServiceService.class);
    private static final List<String> CHANNELS = List.of(
            "home", "chongqing", "sanxia", "xisha", "train", "team", "tour", "order", "user"
    );
    private static final long CACHE_MILLIS = 60_000L;
    private static final long STALE_CACHE_MILLIS = 10 * 60_000L;

    @Resource
    private TourProductSourceConfigService tourProductSourceConfigService;

    @Resource
    private SiteAccessConfigService siteAccessConfigService;

    private final RestTemplate restTemplate;
    private volatile Map<String, Object> cachedConfig = Map.of();
    private volatile String cachedApiBaseUrl = "";
    private volatile long cacheUpdatedAt = 0L;

    public MiniappCustomerServiceService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(4000);
        requestFactory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public Map<String, Object> getPublicConfig() {
        TourProductSourceConfigDTO sourceConfig = tourProductSourceConfigService.getConfig();
        String apiBaseUrl = clean(sourceConfig.getMiniappApiBaseUrl()).replaceAll("/+$", "");
        long now = System.currentTimeMillis();
        boolean sameSource = apiBaseUrl.equals(cachedApiBaseUrl);
        if (sameSource && !cachedConfig.isEmpty() && now - cacheUpdatedAt < CACHE_MILLIS) {
            return copyConfig(cachedConfig);
        }

        try {
            Map<String, Object> config = loadRemoteConfig(apiBaseUrl);
            cachedConfig = immutableConfig(config);
            cachedApiBaseUrl = apiBaseUrl;
            cacheUpdatedAt = now;
            return copyConfig(cachedConfig);
        } catch (RuntimeException ex) {
            if (sameSource && !cachedConfig.isEmpty() && now - cacheUpdatedAt < STALE_CACHE_MILLIS) {
                LOGGER.warn("Miniapp customer service config refresh failed, serving cached data: {}", ex.getMessage());
                return copyConfig(cachedConfig);
            }
            LOGGER.warn("Miniapp customer service config unavailable, using website fallback: {}", ex.getMessage());
            return websiteFallback();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadRemoteConfig(String apiBaseUrl) {
        if (apiBaseUrl.isBlank()) {
            throw new IllegalStateException("尚未配置小程序 API 地址");
        }
        URI uri = URI.create(apiBaseUrl + "/customer-service/config");
        try {
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response == null || !"0".equals(clean(response.get("code")))) {
                throw new IllegalStateException(defaultText(response == null ? null : response.get("message"), "客服接口请求失败"));
            }
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?>)) {
                throw new IllegalStateException("客服接口返回格式不正确");
            }
            return normalizeRemoteConfig((Map<String, Object>) data);
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new IllegalStateException("无法连接小程序客服接口", ex);
        }
    }

    private Map<String, Object> normalizeRemoteConfig(Map<String, Object> source) {
        String mode = clean(source.get("mode")).toLowerCase(Locale.ROOT);
        String serviceUrl = validWecomUrl(source.get("serviceUrl"));
        boolean configured = booleanValue(source.get("configured"));
        boolean enabled = booleanValue(source.get("enabled"))
                && configured
                && "wecom".equals(mode)
                && !serviceUrl.isBlank();

        Map<String, String> channelUrls = new LinkedHashMap<>();
        Map<?, ?> sourceChannels = source.get("channelUrls") instanceof Map<?, ?> map ? map : Map.of();
        for (String channel : CHANNELS) {
            String channelUrl = validWecomUrl(sourceChannels.get(channel));
            if (!channelUrl.isBlank()) {
                channelUrls.put(channel, channelUrl);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("configured", configured && !serviceUrl.isBlank());
        result.put("mode", mode.isBlank() ? "disabled" : mode);
        result.put("displayName", defaultText(source.get("displayName"), "在线客服"));
        result.put("serviceUrl", serviceUrl);
        result.put("channelUrls", channelUrls);
        result.put("source", "MINIAPP");
        return result;
    }

    private Map<String, Object> websiteFallback() {
        SiteAccessConfigDTO config = siteAccessConfigService.getPublicConfig();
        String serviceUrl = validWecomUrl(config.getSupportUrl());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", !serviceUrl.isBlank());
        result.put("configured", !serviceUrl.isBlank());
        result.put("mode", serviceUrl.isBlank() ? "disabled" : "wecom");
        result.put("displayName", defaultText(config.getSupportButtonText(), "在线客服"));
        result.put("serviceUrl", serviceUrl);
        result.put("channelUrls", Map.of());
        result.put("source", "SITE_ACCESS_FALLBACK");
        return result;
    }

    private String validWecomUrl(Object value) {
        String url = clean(value);
        if (url.isBlank()) return "";
        try {
            URI uri = URI.create(url);
            boolean valid = "https".equalsIgnoreCase(uri.getScheme())
                    && "work.weixin.qq.com".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/kfid/");
            return valid ? uri.toString() : "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private Map<String, Object> immutableConfig(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>(source);
        Object channels = source.get("channelUrls");
        result.put("channelUrls", channels instanceof Map<?, ?> map ? Map.copyOf(map) : Map.of());
        return Map.copyOf(result);
    }

    private Map<String, Object> copyConfig(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>(source);
        Object channels = source.get("channelUrls");
        result.put("channelUrls", channels instanceof Map<?, ?> map ? new LinkedHashMap<>(map) : new LinkedHashMap<>());
        return result;
    }

    private boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(clean(value));
    }

    private String defaultText(Object value, String fallback) {
        String text = clean(value);
        return text.isBlank() ? fallback : text;
    }

    private String clean(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
