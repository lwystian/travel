package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "行程详情DTO")
public class TourDetailDTO {
    @Schema(description = "行程基本信息")
    private TourBasicInfo tour;

    @Schema(description = "行程标签列表")
    private List<String> tags;

    @Schema(description = "产品特色列表")
    private List<String> features;

    @Schema(description = "供应商信息")
    private SupplierInfo supplier;

    @Schema(description = "退订政策")
    private RefundPolicy refundPolicy;

    @Schema(description = "行程套餐列表")
    private List<PackageInfo> tripPackages;

    @Schema(description = "批次套餐列表")
    private List<PackageInfo> batchPackages;

    @Schema(description = "出发日期列表")
    private List<BatchDateInfo> batchDates;

    @Schema(description = "图片列表")
    private ImageInfo images;

    @Schema(description = "视频信息")
    private VideoInfo video;

    @Schema(description = "可选酒店列表")
    private List<HotelInfo> availableHotels;

    @Data
    @Schema(description = "行程基本信息")
    public static class TourBasicInfo {
        private Long id;
        private String title;
        private String subtitle;
        private String code;
        private Integer days;
        private String departure;
        private Integer enrolledCount;
        private String notice;
    }

    @Data
    @Schema(description = "供应商信息")
    public static class SupplierInfo {
        private String name;
    }

    @Data
    @Schema(description = "退订政策")
    public static class RefundPolicy {
        private String support;
        private String special;
    }

    @Data
    @Schema(description = "套餐信息")
    public static class PackageInfo {
        private Long id;
        private String name;
        private BigDecimal adultPrice;
        private BigDecimal childPrice;
        private BigDecimal extraFeePerPerson;
        private String description;
    }

    @Data
    @Schema(description = "出发日期信息")
    public static class BatchDateInfo {
        private String date;
        private BigDecimal adultDateExtraFee;
        private BigDecimal childDateExtraFee;
        private String status;
        private Integer remaining;
        private Integer occupied;
    }

    @Data
    @Schema(description = "图片信息")
    public static class ImageInfo {
        private List<String> main;
        private List<String> thumbnails;
    }

    @Data
    @Schema(description = "视频信息")
    public static class VideoInfo {
        private String url;
        private String poster;
        private Integer enabled;
    }

    @Data
    @Schema(description = "可选酒店信息")
    public static class HotelInfo {
        private Long id;
        private Long accommodationId;
        private String name;
        private String type;
        private String priceRange;
        private BigDecimal starLevel;
        private String imageUrl;
    }
}
