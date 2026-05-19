package org.example.springboot.dto;

import lombok.Data;

@Data
public class GeetestConfigDTO {
    private Boolean enabled = false;
    private String captchaId;
    private String captchaKey;
    private Boolean configured = false;
    private String challenge;
    private Boolean success = false;
}
