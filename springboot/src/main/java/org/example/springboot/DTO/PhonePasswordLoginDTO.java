package org.example.springboot.dto;

import lombok.Data;

@Data
public class PhonePasswordLoginDTO {
    private String phone;
    private String encryptedPassword;
    private Boolean agreementAccepted;
}
