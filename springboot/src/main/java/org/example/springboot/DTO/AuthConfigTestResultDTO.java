package org.example.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class AuthConfigTestResultDTO {
    private Boolean success;
    private String title;
    private String message;
    private List<String> checks;
    private LocalDateTime testedAt;

    public static AuthConfigTestResultDTO ok(String title, String message, List<String> checks) {
        return new AuthConfigTestResultDTO(true, title, message, checks, LocalDateTime.now());
    }

    public static AuthConfigTestResultDTO fail(String title, String message, List<String> checks) {
        return new AuthConfigTestResultDTO(false, title, message, checks, LocalDateTime.now());
    }
}
