package org.example.springboot.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.dto.SiteFooterConfigDTO;
import org.example.springboot.entity.AuthProviderConfig;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AuthProviderConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiteFooterConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    private static final String CONFIG_TYPE = "site_footer";
    private static final String CONFIG_NAME = "site footer config";
    private static final String CONFIG_DESCRIPTION = "site footer config";
    private static final int MAX_LINKS = 20;
    private static final int MAX_FEATURES = 8;
    private static final int MAX_QR_CODES = 6;
    private static final int MAX_CERTIFICATES = 12;
    private static final int MAX_LEGAL_NOTES = 10;
    private static final int MAX_LEGAL_PAGES = 6;
    private static final int MAX_LEGAL_SECTIONS = 8;
    private static final int MAX_LEGAL_SECTION_ITEMS = 12;

    public SiteFooterConfigDTO getPublicConfig() {
        AuthProviderConfig entity = getOrCreate();
        SiteFooterConfigDTO dto = parse(entity.getConfigData());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        normalize(dto);
        return dto;
    }

    public SiteFooterConfigDTO getAdminConfig() {
        return getPublicConfig();
    }

    @Transactional
    public void saveConfig(SiteFooterConfigDTO dto) {
        if (dto == null) {
            throw new ServiceException("页脚配置不能为空");
        }
        normalize(dto);
        AuthProviderConfig entity = getOrCreate();
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        entity.setConfigData(JSON.toJSONString(dto));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setUpdatedAt(LocalDateTime.now());
        if (!updateById(entity)) {
            throw new ServiceException("保存网站页脚配置失败");
        }
    }

    private void normalize(SiteFooterConfigDTO dto) {
        if (dto.getEnabled() == null) {
            dto.setEnabled(false);
        }
        dto.setCompanyName(clean(dto.getCompanyName(), 80));
        dto.setBrandName(clean(dto.getBrandName(), 40));
        dto.setSlogan(clean(dto.getSlogan(), 120));
        dto.setConsultationPhone(clean(dto.getConsultationPhone(), 40));
        dto.setCruisePhone(clean(dto.getCruisePhone(), 40));
        dto.setServiceTime(clean(dto.getServiceTime(), 80));
        dto.setAddress(clean(dto.getAddress(), 180));
        dto.setCopyright(clean(dto.getCopyright(), 120));
        dto.setIcpNumber(clean(dto.getIcpNumber(), 80));
        dto.setIcpUrl(normalizeUrl(dto.getIcpUrl()));
        dto.setPoliceNumber(clean(dto.getPoliceNumber(), 80));
        dto.setPoliceUrl(normalizeUrl(dto.getPoliceUrl()));
        dto.setLicenseNumber(clean(dto.getLicenseNumber(), 80));
        dto.setComplaintPhone(clean(dto.getComplaintPhone(), 40));
        dto.setTechnicalSupport(clean(dto.getTechnicalSupport(), 80));
        dto.setReportEmail(clean(dto.getReportEmail(), 80));
        dto.setMinorReportEmail(clean(dto.getMinorReportEmail(), 80));
        dto.setBusinessLicenseImageUrl(normalizeImagePath(dto.getBusinessLicenseImageUrl()));
        dto.setTravelLicenseImageUrl(normalizeImagePath(dto.getTravelLicenseImageUrl()));
        dto.setFieldLabels(normalizeFieldLabels(dto.getFieldLabels()));
        dto.setFeatureItems(normalizeFeatures(dto.getFeatureItems()));
        dto.setTopLinks(normalizeLinks(dto.getTopLinks(), MAX_LINKS));
        dto.setComplianceLinks(normalizeLinks(dto.getComplianceLinks(), MAX_LINKS));
        dto.setFriendlyLinks(normalizeLinks(dto.getFriendlyLinks(), MAX_LINKS));
        dto.setQrCodes(normalizeQrCodes(dto.getQrCodes()));
        dto.setCertificates(normalizeCertificates(dto.getCertificates()));
        dto.setLegalNotes(normalizeLegalNotes(dto.getLegalNotes()));
        dto.setLegalPages(normalizeLegalPages(dto.getLegalPages()));
        syncQualificationImages(dto);
    }

    private void syncQualificationImages(SiteFooterConfigDTO dto) {
        if (dto.getLegalPages() == null) {
            return;
        }
        for (SiteFooterConfigDTO.LegalPageItem page : dto.getLegalPages()) {
            if (page == null) {
                continue;
            }
            if ("business-license".equals(page.getType())) {
                if (!StringUtils.hasText(page.getImageUrl()) && StringUtils.hasText(dto.getBusinessLicenseImageUrl())) {
                    page.setImageUrl(dto.getBusinessLicenseImageUrl());
                }
                dto.setBusinessLicenseImageUrl(page.getImageUrl());
            }
            if ("travel-license".equals(page.getType())) {
                if (!StringUtils.hasText(page.getImageUrl()) && StringUtils.hasText(dto.getTravelLicenseImageUrl())) {
                    page.setImageUrl(dto.getTravelLicenseImageUrl());
                }
                dto.setTravelLicenseImageUrl(page.getImageUrl());
            }
        }
    }

    private Map<String, String> normalizeFieldLabels(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = clean(entry.getKey(), 40);
            String value = clean(entry.getValue(), 30);
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                result.put(key, value);
            }
        }
        return result;
    }

    private List<SiteFooterConfigDTO.LegalPageItem> normalizeLegalPages(List<SiteFooterConfigDTO.LegalPageItem> source) {
        List<SiteFooterConfigDTO.LegalPageItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (SiteFooterConfigDTO.LegalPageItem item : source) {
            if (item == null) {
                continue;
            }
            SiteFooterConfigDTO.LegalPageItem normalized = new SiteFooterConfigDTO.LegalPageItem();
            normalized.setType(clean(item.getType(), 40));
            normalized.setTitle(clean(item.getTitle(), 40));
            normalized.setNavText(clean(item.getNavText(), 60));
            normalized.setKicker(clean(item.getKicker(), 30));
            normalized.setHeading(clean(item.getHeading(), 80));
            normalized.setSummary(clean(item.getSummary(), 180));
            normalized.setLead(clean(item.getLead(), 500));
            normalized.setNotice(clean(item.getNotice(), 240));
            normalized.setQualificationTitle(clean(item.getQualificationTitle(), 60));
            normalized.setImageUrl(normalizeImagePath(item.getImageUrl()));
            normalized.setSections(normalizeLegalSections(item.getSections()));
            result.add(normalized);
            if (result.size() >= MAX_LEGAL_PAGES) {
                break;
            }
        }
        return result;
    }

    private List<SiteFooterConfigDTO.LegalSectionItem> normalizeLegalSections(List<SiteFooterConfigDTO.LegalSectionItem> source) {
        List<SiteFooterConfigDTO.LegalSectionItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (SiteFooterConfigDTO.LegalSectionItem section : source) {
            if (section == null) {
                continue;
            }
            SiteFooterConfigDTO.LegalSectionItem normalized = new SiteFooterConfigDTO.LegalSectionItem();
            normalized.setTitle(clean(section.getTitle(), 60));
            normalized.setText(clean(section.getText(), 500));
            normalized.setItems(normalizeLegalSectionItems(section.getItems()));
            result.add(normalized);
            if (result.size() >= MAX_LEGAL_SECTIONS) {
                break;
            }
        }
        return result;
    }

    private List<String> normalizeLegalSectionItems(List<String> source) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (String item : source) {
            String normalized = clean(item, 240);
            if (StringUtils.hasText(normalized)) {
                result.add(normalized);
            }
            if (result.size() >= MAX_LEGAL_SECTION_ITEMS) {
                break;
            }
        }
        return result;
    }

    private List<SiteFooterConfigDTO.FeatureItem> normalizeFeatures(List<SiteFooterConfigDTO.FeatureItem> source) {
        List<SiteFooterConfigDTO.FeatureItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (SiteFooterConfigDTO.FeatureItem item : source) {
            if (item == null) {
                continue;
            }
            SiteFooterConfigDTO.FeatureItem normalized = new SiteFooterConfigDTO.FeatureItem();
            normalized.setTitle(clean(item.getTitle(), 20));
            normalized.setDescription(clean(item.getDescription(), 80));
            normalized.setIcon(clean(item.getIcon(), 30));
            result.add(normalized);
            if (result.size() >= MAX_FEATURES) {
                break;
            }
        }
        return result;
    }

    private List<SiteFooterConfigDTO.LinkItem> normalizeLinks(List<SiteFooterConfigDTO.LinkItem> source, int limit) {
        List<SiteFooterConfigDTO.LinkItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (SiteFooterConfigDTO.LinkItem item : source) {
            if (item == null) {
                continue;
            }
            SiteFooterConfigDTO.LinkItem normalized = new SiteFooterConfigDTO.LinkItem();
            normalized.setLabel(clean(item.getLabel(), 30));
            normalized.setUrl(normalizeUrl(item.getUrl()));
            result.add(normalized);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<SiteFooterConfigDTO.QrCodeItem> normalizeQrCodes(List<SiteFooterConfigDTO.QrCodeItem> source) {
        List<SiteFooterConfigDTO.QrCodeItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (SiteFooterConfigDTO.QrCodeItem item : source) {
            if (item == null) {
                continue;
            }
            SiteFooterConfigDTO.QrCodeItem normalized = new SiteFooterConfigDTO.QrCodeItem();
            normalized.setLabel(clean(item.getLabel(), 30));
            normalized.setImageUrl(normalizeImagePath(item.getImageUrl()));
            normalized.setDescription(clean(item.getDescription(), 60));
            result.add(normalized);
            if (result.size() >= MAX_QR_CODES) {
                break;
            }
        }
        return result;
    }

    private List<SiteFooterConfigDTO.CertificateItem> normalizeCertificates(List<SiteFooterConfigDTO.CertificateItem> source) {
        List<SiteFooterConfigDTO.CertificateItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (SiteFooterConfigDTO.CertificateItem item : source) {
            if (item == null) {
                continue;
            }
            SiteFooterConfigDTO.CertificateItem normalized = new SiteFooterConfigDTO.CertificateItem();
            normalized.setTitle(clean(item.getTitle(), 40));
            normalized.setImageUrl(normalizeImagePath(item.getImageUrl()));
            normalized.setDescription(clean(item.getDescription(), 80));
            result.add(normalized);
            if (result.size() >= MAX_CERTIFICATES) {
                break;
            }
        }
        return result;
    }

    private List<String> normalizeLegalNotes(List<String> source) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (String note : source) {
            String normalized = clean(note, 180);
            if (StringUtils.hasText(normalized)) {
                result.add(normalized);
            }
            if (result.size() >= MAX_LEGAL_NOTES) {
                break;
            }
        }
        return result;
    }

    private String clean(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim().replaceAll("[\\r\\n\\t]+", " ");
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private String normalizeUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = clean(value, 300);
        if (cleaned.startsWith("/") || cleaned.startsWith("#")
                || cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return cleaned;
        }
        return "";
    }

    private String normalizeImagePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = clean(value, 300);
        if (cleaned.startsWith("/") || cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return cleaned;
        }
        return "";
    }

    private SiteFooterConfigDTO parse(String json) {
        if (!StringUtils.hasText(json)) {
            return new SiteFooterConfigDTO();
        }
        try {
            SiteFooterConfigDTO parsed = JSON.parseObject(json, SiteFooterConfigDTO.class);
            return parsed == null ? new SiteFooterConfigDTO() : parsed;
        } catch (Exception e) {
            return new SiteFooterConfigDTO();
        }
    }

    private AuthProviderConfig getOrCreate() {
        AuthProviderConfig entity = getOne(new LambdaQueryWrapper<AuthProviderConfig>()
                .eq(AuthProviderConfig::getConfigType, CONFIG_TYPE));
        if (entity != null) {
            return entity;
        }
        entity = new AuthProviderConfig();
        entity.setConfigType(CONFIG_TYPE);
        entity.setConfigName(CONFIG_NAME);
        entity.setEnabled(false);
        entity.setConfigData(JSON.toJSONString(new SiteFooterConfigDTO()));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);
        return entity;
    }
}
