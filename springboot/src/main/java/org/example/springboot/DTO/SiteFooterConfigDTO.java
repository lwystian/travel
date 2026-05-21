package org.example.springboot.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SiteFooterConfigDTO {
    private Boolean enabled;
    private String companyName;
    private String brandName;
    private String slogan;
    private String consultationPhone;
    private String cruisePhone;
    private String serviceTime;
    private String address;
    private String copyright;
    private String icpNumber;
    private String icpUrl;
    private String policeNumber;
    private String policeUrl;
    private String licenseNumber;
    private String complaintPhone;
    private String technicalSupport;
    private String reportEmail;
    private String minorReportEmail;
    private List<FeatureItem> featureItems = new ArrayList<>();
    private List<LinkItem> topLinks = new ArrayList<>();
    private List<LinkItem> complianceLinks = new ArrayList<>();
    private List<LinkItem> friendlyLinks = new ArrayList<>();
    private List<QrCodeItem> qrCodes = new ArrayList<>();
    private List<CertificateItem> certificates = new ArrayList<>();
    private List<String> legalNotes = new ArrayList<>();

    @Data
    public static class LinkItem {
        private String label;
        private String url;
    }

    @Data
    public static class FeatureItem {
        private String title;
        private String description;
        private String icon;
    }

    @Data
    public static class QrCodeItem {
        private String label;
        private String imageUrl;
        private String description;
    }

    @Data
    public static class CertificateItem {
        private String title;
        private String imageUrl;
        private String description;
    }
}
