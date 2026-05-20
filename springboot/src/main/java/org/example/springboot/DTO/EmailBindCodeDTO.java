package org.example.springboot.dto;

import lombok.Data;

@Data
public class EmailBindCodeDTO {
    private String email;
    private GeetestVerifyDTO geetest;
}
