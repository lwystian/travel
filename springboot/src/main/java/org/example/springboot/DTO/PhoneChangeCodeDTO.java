package org.example.springboot.dto;

import lombok.Data;

@Data
public class PhoneChangeCodeDTO {
    private String phone;
    private GeetestVerifyDTO geetest;
}
