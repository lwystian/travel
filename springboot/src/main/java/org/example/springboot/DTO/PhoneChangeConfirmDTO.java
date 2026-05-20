package org.example.springboot.dto;

import lombok.Data;

@Data
public class PhoneChangeConfirmDTO {
    private String phone;
    private String smsCode;
    private String currentSmsCode;
}
