package org.example.springboot.dto;

import lombok.Data;

@Data
public class EmailBindConfirmDTO {
    private String email;
    private String code;
}
