package org.example.springboot.dto;

import lombok.Data;

@Data
public class AuthConfigTestRequestDTO {
    private String phone;
    private String email;
    private String templateCode;
}
