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
import java.util.List;

@Service
public class SiteFooterConfigService extends ServiceImpl<AuthProviderConfigMapper, AuthProviderConfig> {
    private static final String CONFIG_TYPE = "site_footer";
    private static final String CONFIG_NAME = "网站页脚配置";
    private static final String CONFIG_DESCRIPTION = "用于网站企业化页脚展示，包括备案、二维码、证书和友情链接";
    private static final int MAX_LINKS = 20;
    private static final int MAX_FEATURES = 8;
    private static final int MAX_QR_CODES = 6;
    private static final int MAX_CERTIFICATES = 12;
    private static final int MAX_LEGAL_NOTES = 10;

    public SiteFooterConfigDTO getPublicConfig() {
        AuthProviderConfig entity = getOrCreate();
        SiteFooterConfigDTO dto = parse(entity.getConfigData());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        fillDefaults(dto);
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
        fillDefaults(dto);
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
        dto.setFeatureItems(normalizeFeatures(dto.getFeatureItems()));
        dto.setTopLinks(normalizeLinks(dto.getTopLinks(), MAX_LINKS));
        dto.setComplianceLinks(normalizeLinks(dto.getComplianceLinks(), MAX_LINKS));
        dto.setFriendlyLinks(normalizeLinks(dto.getFriendlyLinks(), MAX_LINKS));
        dto.setQrCodes(normalizeQrCodes(dto.getQrCodes()));
        dto.setCertificates(normalizeCertificates(dto.getCertificates()));
        dto.setLegalNotes(normalizeLegalNotes(dto.getLegalNotes()));
    }

    private void fillDefaults(SiteFooterConfigDTO dto) {
        if (dto.getEnabled() == null) {
            dto.setEnabled(true);
        }
        if (!StringUtils.hasText(dto.getCompanyName())) {
            dto.setCompanyName("侠客行国际旅行社有限公司");
        }
        if (!StringUtils.hasText(dto.getBrandName())) {
            dto.setBrandName("侠客行国旅");
        }
        if (!StringUtils.hasText(dto.getSlogan())) {
            dto.setSlogan("扎根重庆，连接山城、三峡与西南山河的品质旅行服务");
        }
        if (!StringUtils.hasText(dto.getConsultationPhone())) {
            dto.setConsultationPhone("400-800-5178");
        }
        if (!StringUtils.hasText(dto.getCruisePhone())) {
            dto.setCruisePhone("023-6789-5178");
        }
        if (!StringUtils.hasText(dto.getServiceTime())) {
            dto.setServiceTime("09:00 - 20:00，节假日专人在线");
        }
        if (!StringUtils.hasText(dto.getAddress())) {
            dto.setAddress("重庆市渝中区解放碑商圈时代旅行中心 18F");
        }
        if (!StringUtils.hasText(dto.getCopyright())) {
            dto.setCopyright("© 2021-2026 侠客行国旅 版权所有");
        }
        if (!StringUtils.hasText(dto.getLicenseNumber())) {
            dto.setLicenseNumber("L-CQ-XXK-2026");
        }
        if (!StringUtils.hasText(dto.getComplaintPhone())) {
            dto.setComplaintPhone("12345 / 023-6789-5178");
        }
        if (!StringUtils.hasText(dto.getTechnicalSupport())) {
            dto.setTechnicalSupport("侠客行数字旅行中心");
        }
        if (!StringUtils.hasText(dto.getReportEmail())) {
            dto.setReportEmail("service@xkxtrip.com");
        }
        if (!StringUtils.hasText(dto.getMinorReportEmail())) {
            dto.setMinorReportEmail("safe@xkxtrip.com");
        }
        if (dto.getTopLinks() == null || dto.getTopLinks().isEmpty()) {
            dto.setTopLinks(defaultTopLinks());
        }
        if (dto.getComplianceLinks() == null || dto.getComplianceLinks().isEmpty()) {
            dto.setComplianceLinks(defaultComplianceLinks());
        }
        if (dto.getFeatureItems() == null || dto.getFeatureItems().isEmpty()) {
            dto.setFeatureItems(defaultFeatures());
        }
        if (dto.getFriendlyLinks() == null) {
            dto.setFriendlyLinks(new ArrayList<>());
        }
        if (dto.getQrCodes() == null || dto.getQrCodes().isEmpty()) {
            dto.setQrCodes(defaultQrCodes());
        }
        if (dto.getCertificates() == null) {
            dto.setCertificates(new ArrayList<>());
        }
        if (dto.getLegalNotes() == null || dto.getLegalNotes().isEmpty()) {
            dto.setLegalNotes(List.of("平台展示的图片、攻略与目的地资料仅用于旅行服务说明，出行前请以订单确认信息和当地实时政策为准。"));
        }
    }

    private List<SiteFooterConfigDTO.LinkItem> defaultTopLinks() {
        return List.of(
                link("关于侠客行", "/about"),
                link("山城线路", "/tickets"),
                link("目的地灵感", "/scenic"),
                link("旅行攻略", "/guide"),
                link("服务承诺", "/about"),
                link("联系我们", "/about")
        );
    }

    private List<SiteFooterConfigDTO.LinkItem> defaultComplianceLinks() {
        return List.of(
                link("营业执照", "/about"),
                link("旅行社业务经营许可", "/about"),
                link("服务规范", "/about"),
                link("社区公约", "/about"),
                link("安全与隐私保护", "/about"),
                link("在线服务与投诉反馈专区", "/about")
        );
    }

    private List<SiteFooterConfigDTO.FeatureItem> defaultFeatures() {
        return List.of(
                feature("守信", "行程说明、费用明细与服务标准清楚呈现，重要节点可追踪。", "shield"),
                feature("甄选", "围绕重庆、三峡与西南目的地，打磨小而美的品质线路。", "route"),
                feature("陪伴", "顾问、导游与客服协同响应，从预订到返程持续跟进。", "service"),
                feature("自在", "控制车程、住宿与游览节奏，让旅途保持轻松和松弛感。", "experience"),
                feature("友好", "尊重当地风俗与自然环境，倡导低打扰、负责任的旅行。", "leaf"),
                feature("共创", "收集真实评价与旅途故事，把反馈转化为下一次优化。", "share")
        );
    }

    private List<SiteFooterConfigDTO.QrCodeItem> defaultQrCodes() {
        SiteFooterConfigDTO.QrCodeItem wechat = new SiteFooterConfigDTO.QrCodeItem();
        wechat.setLabel("侠客行服务号");
        wechat.setDescription("线路上新与出行提醒");
        SiteFooterConfigDTO.QrCodeItem enterpriseWechat = new SiteFooterConfigDTO.QrCodeItem();
        enterpriseWechat.setLabel("旅行顾问");
        enterpriseWechat.setDescription("定制咨询与售后协助");
        return List.of(wechat, enterpriseWechat);
    }

    private SiteFooterConfigDTO.LinkItem link(String label, String url) {
        SiteFooterConfigDTO.LinkItem item = new SiteFooterConfigDTO.LinkItem();
        item.setLabel(label);
        item.setUrl(url);
        return item;
    }

    private SiteFooterConfigDTO.FeatureItem feature(String title, String description, String icon) {
        SiteFooterConfigDTO.FeatureItem item = new SiteFooterConfigDTO.FeatureItem();
        item.setTitle(title);
        item.setDescription(description);
        item.setIcon(icon);
        return item;
    }

    private List<SiteFooterConfigDTO.FeatureItem> normalizeFeatures(List<SiteFooterConfigDTO.FeatureItem> source) {
        List<SiteFooterConfigDTO.FeatureItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (SiteFooterConfigDTO.FeatureItem item : source) {
            if (item == null || !StringUtils.hasText(item.getTitle())) {
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
            if (item == null || !StringUtils.hasText(item.getLabel())) {
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
            if (item == null || !StringUtils.hasText(item.getLabel())) {
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
            if (item == null || !StringUtils.hasText(item.getTitle())) {
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
            if (!StringUtils.hasText(note)) {
                continue;
            }
            result.add(clean(note, 180));
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
        entity.setEnabled(true);
        entity.setConfigData(JSON.toJSONString(new SiteFooterConfigDTO()));
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);
        return entity;
    }
}
