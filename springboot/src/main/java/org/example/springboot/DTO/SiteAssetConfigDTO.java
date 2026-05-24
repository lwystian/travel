package org.example.springboot.dto;

import lombok.Data;

@Data
public class SiteAssetConfigDTO {
    private String faviconUrl = "";
    private String logoUrl = "";
    private String wechatQrUrl = "";
    private String authBackgroundUrl = "";
    private String aboutHeroUrl = "";
    private String legalHeroUrl = "";
    private String accommodationHeroUrl = "";
    private String placeholderImageUrl = "";
}
