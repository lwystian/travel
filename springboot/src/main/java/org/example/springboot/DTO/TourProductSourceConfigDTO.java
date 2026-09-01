package org.example.springboot.dto;

import lombok.Data;

@Data
public class TourProductSourceConfigDTO {
    private String sourceMode = "LOCAL";
    private String miniappApiBaseUrl = "";
    private String miniappBookingUrlTemplate = "";
    private Boolean fallbackToLocal = true;
}
