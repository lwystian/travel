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
    private static final String CONFIG_NAME = "网站页脚配置";
    private static final String CONFIG_DESCRIPTION = "用于网站企业化页脚展示，包括备案、二维码、证书和友情链接";
    private static final int MAX_LINKS = 20;
    private static final int MAX_FEATURES = 8;
    private static final int MAX_QR_CODES = 6;
    private static final int MAX_CERTIFICATES = 12;
    private static final int MAX_LEGAL_NOTES = 10;
    private static final int MAX_LEGAL_PAGES = 6;
    private static final int MAX_LEGAL_SECTIONS = 8;
    private static final int MAX_LEGAL_SECTION_ITEMS = 12;
    private static final Map<String, String> COMPLIANCE_LINK_ROUTES = Map.of(
            "营业执照", "/legal/business-license",
            "旅行社业务经营许可", "/legal/travel-license",
            "服务规范", "/legal/service-standard",
            "社区公约", "/legal/community-guidelines",
            "安全与隐私保护", "/legal/privacy-safety",
            "在线服务与投诉反馈专区", "/legal/support-feedback"
    );

    public SiteFooterConfigDTO getPublicConfig() {
        AuthProviderConfig entity = getOrCreate();
        SiteFooterConfigDTO dto = parse(entity.getConfigData());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        fillDefaults(dto);
        dto.setComplianceLinks(normalizeComplianceLinks(dto.getComplianceLinks()));
        dto.setLegalPages(normalizeLegalPages(dto.getLegalPages()));
        syncQualificationImages(dto);
        persistNormalizedConfigIfNeeded(entity, dto);
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
        dto.setBusinessLicenseImageUrl(normalizeImagePath(dto.getBusinessLicenseImageUrl()));
        dto.setTravelLicenseImageUrl(normalizeImagePath(dto.getTravelLicenseImageUrl()));
        dto.setFieldLabels(normalizeFieldLabels(dto.getFieldLabels()));
        dto.setFeatureItems(normalizeFeatures(dto.getFeatureItems()));
        dto.setTopLinks(normalizeLinks(dto.getTopLinks(), MAX_LINKS));
        dto.setComplianceLinks(normalizeComplianceLinks(dto.getComplianceLinks()));
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

    private void persistNormalizedConfigIfNeeded(AuthProviderConfig entity, SiteFooterConfigDTO dto) {
        String normalizedConfigData = JSON.toJSONString(dto);
        if (normalizedConfigData.equals(entity.getConfigData())) {
            return;
        }
        entity.setConfigName(CONFIG_NAME);
        entity.setConfigData(normalizedConfigData);
        entity.setDescription(CONFIG_DESCRIPTION);
        entity.setUpdatedAt(LocalDateTime.now());
        updateById(entity);
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
        if (dto.getFieldLabels() == null || dto.getFieldLabels().isEmpty()) {
            dto.setFieldLabels(defaultFieldLabels());
        } else {
            Map<String, String> merged = defaultFieldLabels();
            merged.putAll(dto.getFieldLabels());
            dto.setFieldLabels(merged);
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
        if (dto.getLegalPages() == null || dto.getLegalPages().isEmpty()) {
            dto.setLegalPages(defaultLegalPages());
        } else {
            dto.setLegalPages(mergeLegalPages(dto.getLegalPages()));
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

    private Map<String, String> defaultFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("enabled", "前台展示状态");
        labels.put("companyName", "企业名称");
        labels.put("brandName", "品牌名称");
        labels.put("slogan", "页脚标语");
        labels.put("consultationPhone", "旅行咨询电话");
        labels.put("cruisePhone", "团队与邮轮咨询");
        labels.put("serviceTime", "服务时间");
        labels.put("address", "公司总部地址");
        labels.put("icpNumber", "ICP备案号");
        labels.put("icpUrl", "ICP备案链接");
        labels.put("policeNumber", "公安备案号");
        labels.put("policeUrl", "公安备案链接");
        labels.put("licenseNumber", "许可编号");
        labels.put("complaintPhone", "投诉与服务监督");
        labels.put("technicalSupport", "法律与技术支持");
        labels.put("reportEmail", "服务邮箱");
        labels.put("minorReportEmail", "未成年人信息保护邮箱");
        labels.put("copyright", "版权信息");
        return labels;
    }

    private List<SiteFooterConfigDTO.LinkItem> defaultComplianceLinks() {
        return List.of(
                link("营业执照", "/legal/business-license"),
                link("旅行社业务经营许可", "/legal/travel-license"),
                link("服务规范", "/legal/service-standard"),
                link("社区公约", "/legal/community-guidelines"),
                link("安全与隐私保护", "/legal/privacy-safety"),
                link("在线服务与投诉反馈专区", "/legal/support-feedback")
        );
    }

    private List<SiteFooterConfigDTO.LegalPageItem> defaultLegalPages() {
        return List.of(
                legalPage(
                        "business-license",
                        "营业执照",
                        "企业主体与经营信息",
                        "企业资质",
                        "重庆侠客行国际旅行社有限公司主体说明",
                        "展示企业主体、经营范围与资质核验方式，方便用户识别平台服务主体。",
                        "侠客行国旅依法开展旅行咨询、线路组织、订单服务与售后协助等相关业务。平台展示的企业信息用于帮助用户了解服务主体，具体登记信息以市场监督管理部门公示为准。",
                        "营业执照资质图片",
                        "如需查看营业执照扫描件或进一步核验主体信息，可联系平台客服或通过官方公开渠道查询。",
                        List.of(
                                legalSection("主体信息", "", List.of("企业名称：重庆侠客行国际旅行社有限公司", "品牌简称：侠客行国旅", "服务定位：重庆及西南目的地品质旅行服务、团队出行与定制咨询", "总部地址：重庆市渝中区解放碑商圈时代旅行中心 18F")),
                                legalSection("核验方式", "用户可通过国家企业信用信息公示系统、地方市场监督管理部门公开渠道，核验企业登记状态、统一社会信用代码、经营范围和行政许可信息。", List.of())
                        )
                ),
                legalPage(
                        "travel-license",
                        "旅行社业务经营许可",
                        "旅行社许可与业务边界",
                        "经营许可",
                        "旅行社业务经营许可说明",
                        "说明旅行社业务经营许可、服务范围、订单确认与履约边界。",
                        "平台围绕旅行社业务许可范围提供线路咨询、行程组织、产品预订与出行服务。涉及交通、住宿、景区、邮轮、保险等第三方服务时，以订单确认信息、供应商规则和目的地实时政策为准。",
                        "旅行社业务经营许可资质图片",
                        "线路页面、攻略和图片仅作服务说明，最终安排以合同、订单确认单和出行通知为准。",
                        List.of(
                                legalSection("许可信息", "", List.of("许可编号：L-CQ-XXK-2026", "业务范围：境内旅游、入境旅游相关咨询与组织服务", "服务区域：以重庆、三峡及西南目的地为核心，覆盖平台实际发布线路")),
                                legalSection("订单确认", "用户提交订单后，平台将根据库存、成团情况、供应商确认、实名信息和支付状态进行确认。未完成确认前，页面展示内容不视为最终履约承诺。", List.of())
                        )
                ),
                legalPage(
                        "service-standard",
                        "服务规范",
                        "预订、履约与售后标准",
                        "服务标准",
                        "侠客行国旅服务规范",
                        "明确咨询、预订、出行、变更、退款和售后处理的基本流程。",
                        "我们希望每一次旅行服务都清楚、可追踪、可沟通。平台通过订单记录、客服跟进和出行通知，帮助用户理解服务进度与权责边界。",
                        "",
                        "涉及退改、赔付或补偿事项，以订单规则、合同约定及实际责任认定为准。",
                        List.of(
                                legalSection("咨询与预订", "", List.of("展示线路亮点、价格说明、费用包含与不包含项目", "重要限制条件、年龄要求、证件要求和取消规则应在下单前提示", "客服或旅行顾问应基于用户需求提供真实、完整、不过度承诺的建议")),
                                legalSection("出行履约", "", List.of("出行前发送集合信息、联系人、注意事项和必要材料提醒", "行程中如遇天气、交通、景区管控等不可控因素，将优先保障人身安全并协调替代方案"))
                        )
                ),
                legalPage(
                        "community-guidelines",
                        "社区公约",
                        "评价、攻略与互动规则",
                        "社区秩序",
                        "侠客行社区公约",
                        "维护真实、友好、有帮助的旅行内容环境。",
                        "平台鼓励用户分享真实体验、实用攻略和建设性建议，也会保护旅行者、商家、导游和目的地社区的合法权益。",
                        "",
                        "如你认为内容被误判，可通过意见反馈或客服渠道提交申诉材料。",
                        List.of(
                                legalSection("鼓励发布", "", List.of("真实旅行经历、实用路线建议、避坑提醒和消费体验", "清晰的图片、时间、交通、住宿、餐饮和目的地信息", "对服务改进有帮助的理性反馈")),
                                legalSection("禁止内容", "", List.of("虚假评价、刷单引流、恶意诋毁或冒充他人", "泄露他人隐私、证件、联系方式、订单信息或未成年人个人信息", "违法违规、低俗辱骂、歧视攻击、危险行为引导或破坏公共秩序的内容"))
                        )
                ),
                legalPage(
                        "privacy-safety",
                        "安全与隐私保护",
                        "数据、账号与未成年人保护",
                        "隐私安全",
                        "安全与隐私保护说明",
                        "说明平台如何保护账号安全、订单信息、联系方式与未成年人信息。",
                        "平台仅在实现注册登录、订单履约、客户服务、风险控制和法律合规所必需的范围内处理用户信息，并采取合理措施保护数据安全。",
                        "",
                        "隐私或未成年人信息相关问题可联系 safe@xkxtrip.com；账号和订单服务可联系 service@xkxtrip.com。",
                        List.of(
                                legalSection("信息使用范围", "", List.of("账号注册、短信验证、身份识别和登录安全", "订单预订、出行通知、客服沟通、发票或合同处理", "投诉处理、风险识别、内容审核和法律法规要求的保存")),
                                legalSection("安全措施", "", List.of("重要操作采用验证码、权限控制或登录态校验", "后台管理按角色控制访问范围，减少非必要信息暴露", "敏感信息处理遵循最小必要原则"))
                        )
                ),
                legalPage(
                        "support-feedback",
                        "在线服务与投诉反馈专区",
                        "客服、投诉与监督渠道",
                        "服务反馈",
                        "在线服务与投诉反馈专区",
                        "提供咨询、售后、投诉、监督和紧急问题处理渠道。",
                        "如果你在咨询、预订、支付、出行或售后过程中遇到问题，可以通过平台客服、电话、邮箱或订单记录提交反馈。我们会根据问题类型分级处理。",
                        "联系图片",
                        "服务时间为 09:00 - 20:00，节假日专人在线；紧急出行问题将优先处理。",
                        List.of(
                                legalSection("联系方式", "", List.of("服务邮箱：service@xkxtrip.com", "未成年人信息保护邮箱：safe@xkxtrip.com", "旅行咨询电话：400-800-5178", "团队与邮轮咨询：023-6789-5178", "投诉与服务监督：12345 / 023-6789-5178")),
                                legalSection("处理流程", "", List.of("普通咨询：按服务时间尽快响应", "订单售后：结合订单号、支付记录、出行凭证和沟通记录核查", "投诉监督：由专人复核并给出处理意见或进一步沟通安排"))
                        )
                )
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

    private SiteFooterConfigDTO.LegalPageItem legalPage(String type, String title, String navText, String kicker,
                                                        String heading, String summary, String lead,
                                                        String qualificationTitle, String notice,
                                                        List<SiteFooterConfigDTO.LegalSectionItem> sections) {
        SiteFooterConfigDTO.LegalPageItem item = new SiteFooterConfigDTO.LegalPageItem();
        item.setType(type);
        item.setTitle(title);
        item.setNavText(navText);
        item.setKicker(kicker);
        item.setHeading(heading);
        item.setSummary(summary);
        item.setLead(lead);
        item.setQualificationTitle(qualificationTitle);
        item.setNotice(notice);
        item.setSections(sections);
        return item;
    }

    private SiteFooterConfigDTO.LegalSectionItem legalSection(String title, String text, List<String> items) {
        SiteFooterConfigDTO.LegalSectionItem section = new SiteFooterConfigDTO.LegalSectionItem();
        section.setTitle(title);
        section.setText(text);
        section.setItems(items);
        return section;
    }

    private Map<String, String> normalizeFieldLabels(Map<String, String> source) {
        Map<String, String> defaults = defaultFieldLabels();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String configured = source == null ? null : source.get(entry.getKey());
            result.put(entry.getKey(), StringUtils.hasText(configured) ? clean(configured, 30) : entry.getValue());
        }
        return result;
    }

    private List<SiteFooterConfigDTO.LegalPageItem> mergeLegalPages(List<SiteFooterConfigDTO.LegalPageItem> source) {
        Map<String, SiteFooterConfigDTO.LegalPageItem> configured = new LinkedHashMap<>();
        if (source != null) {
            for (SiteFooterConfigDTO.LegalPageItem item : source) {
                if (item != null && StringUtils.hasText(item.getType())) {
                    configured.put(item.getType(), item);
                }
            }
        }
        List<SiteFooterConfigDTO.LegalPageItem> result = new ArrayList<>();
        for (SiteFooterConfigDTO.LegalPageItem defaults : defaultLegalPages()) {
            SiteFooterConfigDTO.LegalPageItem item = configured.get(defaults.getType());
            result.add(item == null ? defaults : mergeLegalPage(defaults, item));
        }
        return result;
    }

    private SiteFooterConfigDTO.LegalPageItem mergeLegalPage(SiteFooterConfigDTO.LegalPageItem defaults, SiteFooterConfigDTO.LegalPageItem source) {
        SiteFooterConfigDTO.LegalPageItem item = new SiteFooterConfigDTO.LegalPageItem();
        item.setType(defaults.getType());
        item.setTitle(StringUtils.hasText(source.getTitle()) ? source.getTitle() : defaults.getTitle());
        item.setNavText(StringUtils.hasText(source.getNavText()) ? source.getNavText() : defaults.getNavText());
        item.setKicker(StringUtils.hasText(source.getKicker()) ? source.getKicker() : defaults.getKicker());
        item.setHeading(StringUtils.hasText(source.getHeading()) ? source.getHeading() : defaults.getHeading());
        item.setSummary(StringUtils.hasText(source.getSummary()) ? source.getSummary() : defaults.getSummary());
        item.setLead(StringUtils.hasText(source.getLead()) ? source.getLead() : defaults.getLead());
        item.setNotice(StringUtils.hasText(source.getNotice()) ? source.getNotice() : defaults.getNotice());
        item.setQualificationTitle(StringUtils.hasText(source.getQualificationTitle()) ? source.getQualificationTitle() : defaults.getQualificationTitle());
        item.setImageUrl(StringUtils.hasText(source.getImageUrl()) ? source.getImageUrl() : defaults.getImageUrl());
        item.setSections(source.getSections() == null || source.getSections().isEmpty() ? defaults.getSections() : source.getSections());
        return item;
    }

    private List<SiteFooterConfigDTO.LegalPageItem> normalizeLegalPages(List<SiteFooterConfigDTO.LegalPageItem> source) {
        List<SiteFooterConfigDTO.LegalPageItem> merged = mergeLegalPages(source);
        List<SiteFooterConfigDTO.LegalPageItem> result = new ArrayList<>();
        for (SiteFooterConfigDTO.LegalPageItem item : merged) {
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
            if (section == null || !StringUtils.hasText(section.getTitle())) {
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
            if (!StringUtils.hasText(item)) {
                continue;
            }
            result.add(clean(item, 240));
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

    private List<SiteFooterConfigDTO.LinkItem> normalizeComplianceLinks(List<SiteFooterConfigDTO.LinkItem> source) {
        List<SiteFooterConfigDTO.LinkItem> links = normalizeLinks(source, MAX_LINKS);
        for (SiteFooterConfigDTO.LinkItem link : links) {
            String route = COMPLIANCE_LINK_ROUTES.get(link.getLabel());
            if (StringUtils.hasText(route)) {
                link.setUrl(route);
            }
        }
        return links;
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
