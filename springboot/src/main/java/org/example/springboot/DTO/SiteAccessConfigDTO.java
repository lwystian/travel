package org.example.springboot.dto;

import lombok.Data;

@Data
public class SiteAccessConfigDTO {
    private Boolean siteEnabled;
    private Boolean rejectMobile;
    private Boolean publicInteractionEnabled;
    private String closedTitle = "";
    private String closedMessage = "";
    private String closedContact = "";
    private String mobileTitle = "";
    private String mobileMessage = "";
    private String mobileContact = "";
    private String supportButtonText = "";
    private String supportUrl = "";
    private String supportCredential = "";
    private String supportQrImageUrl = "";
}
