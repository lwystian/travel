package org.example.springboot.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminPermissionUpdateDTO {
    private List<String> permissions;
}
