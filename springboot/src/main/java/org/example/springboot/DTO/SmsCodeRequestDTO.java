package org.example.springboot.dto;

import lombok.Data;

@Data
public class SmsCodeRequestDTO {
    private String phone;
    private String scene;
    private GeetestVerifyDTO geetest;
}
