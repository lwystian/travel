package org.example.springboot.dto;

import lombok.Data;

@Data
public class PhoneCodeLoginDTO {
    private String phone;
    private String smsCode;
    private Boolean agreementAccepted;
}
