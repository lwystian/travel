package org.example.springboot.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CustomerServiceConfigDTO {
    private Boolean enabled = false;
    private String displayName = "在线客服";
    private String corpId = "";
    private String serviceUrl = "";
    private String iconUrl = "";
    private Map<String, String> channelUrls = new LinkedHashMap<>();
}
