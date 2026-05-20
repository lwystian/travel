package org.example.springboot.dto;

import lombok.Data;

@Data
public class EmailSmtpConfigDTO {
    private Boolean enabled = false;
    private String host = "smtp.qq.com";
    private Integer port = 465;
    private String username;
    private String password;
    private String fromEmail;
    private String protocol = "smtps";
    private Boolean sslEnabled = true;
    private Integer codeExpireMinutes = 10;
    private Integer sendIntervalSeconds = 60;
    private Boolean configured = false;
}
