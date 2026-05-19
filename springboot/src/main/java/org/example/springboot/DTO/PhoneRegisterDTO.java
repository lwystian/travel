package org.example.springboot.dto;

import lombok.Data;

@Data
public class PhoneRegisterDTO {
    private String phone;
    private String smsCode;
    private String encryptedPassword;
    private String encryptedConfirmPassword;
    private Boolean agreementAccepted;
}
