package org.example.springboot.dto;

import lombok.Data;

@Data
public class AliyunSmsConfigDTO {
    private Boolean enabled = false;
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;
    private String regionId = "cn-hangzhou";
    private String endpoint = "dysmsapi.aliyuncs.com";
    private Integer codeExpireMinutes = 5;
    private Integer sendIntervalSeconds = 60;
    private Integer dailyLimit = 10;
    private Boolean configured = false;
}
