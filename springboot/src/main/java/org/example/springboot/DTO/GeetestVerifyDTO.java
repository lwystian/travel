package org.example.springboot.dto;

import lombok.Data;

@Data
public class GeetestVerifyDTO {
    private String captchaId;
    private String geetestChallenge;
    private String geetestValidate;
    private String geetestSeccode;
}
