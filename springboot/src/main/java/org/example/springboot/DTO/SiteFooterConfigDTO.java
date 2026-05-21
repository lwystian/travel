package org.example.springboot.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String businessLicenseImageUrl;
    private String travelLicenseImageUrl;
    private Map<String, String> fieldLabels = new HashMap<>();
    private List<FeatureItem> featureItems = new ArrayList<>();
    private List<LinkItem> topLinks = new ArrayList<>();
    private List<LinkItem> complianceLinks = new ArrayList<>();
    private List<LinkItem> friendlyLinks = new ArrayList<>();
    private List<QrCodeItem> qrCodes = new ArrayList<>();
    private List<CertificateItem> certificates = new ArrayList<>();
    private List<String> legalNotes = new ArrayList<>();
    private List<LegalPageItem> legalPages = new ArrayList<>();

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

    @Data
    public static class LegalPageItem {
        private String type;
        private String title;
        private String navText;
        private String kicker;
        private String heading;
        private String summary;
        private String lead;
        private String notice;
        private String qualificationTitle;
        private String imageUrl;
        private List<LegalSectionItem> sections = new ArrayList<>();
    }

    @Data
    public static class LegalSectionItem {
        private String title;
        private String text;
        private List<String> items = new ArrayList<>();
    }
}
