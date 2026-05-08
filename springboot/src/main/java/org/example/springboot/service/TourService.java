package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.DTO.TourDetailDTO;
import org.example.springboot.DTO.HomeRecommendDTO;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TourService {

    @Resource
    private TourMapper tourMapper;

    @Resource
    private TourPackageMapper tourPackageMapper;

    @Resource
    private BatchPackageMapper batchPackageMapper;

    @Resource
    private TourBatchMapper tourBatchMapper;

    @Resource
    private TourHotelMapper tourHotelMapper;

    @Resource
    private HomeRecommendMapper homeRecommendMapper;

    /**
     * 分页查询行程（包含计算最低价格）
     */
    public Page<Tour> getToursByPage(
            String title,
            String tourType,
            String city,
            String destination,
            String theme,
            Integer currentPage,
            Integer size) {
        LambdaQueryWrapper<Tour> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(Tour::getTitle, title);
        }
        if (StringUtils.isNotBlank(tourType)) {
            queryWrapper.eq(Tour::getTourType, tourType);
        }
        if (StringUtils.isNotBlank(city)) {
            queryWrapper.eq(Tour::getCity, city);
        }
        if (StringUtils.isNotBlank(destination)) {
            queryWrapper.eq(Tour::getDestination, destination);
        }
        if (StringUtils.isNotBlank(theme)) {
            queryWrapper.eq(Tour::getTheme, theme);
        }

        // 只查询上架的行程
        queryWrapper.eq(Tour::getStatus, 1);

        // 按创建时间降序排序
        queryWrapper.orderByDesc(Tour::getCreateTime);

        Page<Tour> page = tourMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 为每个行程计算最低价格
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            for (Tour tour : page.getRecords()) {
                BigDecimal minPrice = calculateMinPrice(tour.getId());
                if (minPrice != null) {
                    tour.setMinPrice(minPrice);
                }
            }
        }

        return page;
    }

    /**
     * 获取所有行程（不分页，用于下拉选择等）
     */
    public List<Tour> getAllTours() {
        List<Tour> tours = getActiveTours();
        // 计算每个行程的实际最低价
        for (Tour tour : tours) {
            BigDecimal minPrice = calculateMinPrice(tour.getId());
            if (minPrice != null) {
                tour.setMinPrice(minPrice);
            }
        }
        return tours;
    }

    /**
     * 计算行程的实际最低价
     * 计算公式：行程套餐成人价 + 批次套餐附加费 + 出发班期附加费 的最小组合
     */
    private BigDecimal calculateMinPrice(Long tourId) {
        // 获取所有行程套餐
        List<TourPackage> packages = getTourPackages(tourId);
        if (packages == null || packages.isEmpty()) {
            return null;
        }

        // 获取所有批次套餐的最小附加费
        BigDecimal minBatchExtraFee = getMinBatchExtraFee(tourId);

        // 获取所有出发班期的最小成人附加费
        BigDecimal minDateExtraFee = getMinDateExtraFee(tourId);

        // 计算每个套餐的最低价（套餐价 + 批次附加费 + 日期附加费）
        BigDecimal minPrice = null;
        for (TourPackage pkg : packages) {
            if (pkg.getAdultPrice() != null) {
                BigDecimal totalPrice = pkg.getAdultPrice();
                if (minBatchExtraFee != null) {
                    totalPrice = totalPrice.add(minBatchExtraFee);
                }
                if (minDateExtraFee != null) {
                    totalPrice = totalPrice.add(minDateExtraFee);
                }
                if (minPrice == null || totalPrice.compareTo(minPrice) < 0) {
                    minPrice = totalPrice;
                }
            }
        }

        return minPrice;
    }

    /**
     * 获取所有批次套餐的最小附加费
     */
    private BigDecimal getMinBatchExtraFee(Long tourId) {
        List<BatchPackage> batchPackages = getBatchPackages(tourId);
        BigDecimal minFee = null;
        for (BatchPackage bp : batchPackages) {
            if (bp.getExtraFeePerPerson() != null) {
                if (minFee == null || bp.getExtraFeePerPerson().compareTo(minFee) < 0) {
                    minFee = bp.getExtraFeePerPerson();
                }
            }
        }
        return minFee;
    }

    /**
     * 获取所有出发班期的最小成人附加费
     */
    private BigDecimal getMinDateExtraFee(Long tourId) {
        List<TourBatch> batches = getTourBatches(tourId);
        BigDecimal minFee = null;
        for (TourBatch batch : batches) {
            if (batch.getAdultDateExtraFee() != null) {
                if (minFee == null || batch.getAdultDateExtraFee().compareTo(minFee) < 0) {
                    minFee = batch.getAdultDateExtraFee();
                }
            }
        }
        return minFee;
    }

    /**
     * 根据ID获取行程详情（简单信息）
     */
    public Tour getTourById(Long id) {
        Tour tour = tourMapper.selectById(id);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }
        // 如果没有编码，生成一个
        if (tour.getCode() == null || tour.getCode().isEmpty()) {
            tour.setCode(generateTourCode(tour.getTourType()));
            // 更新数据库
            Tour updateTour = new Tour();
            updateTour.setId(id);
            updateTour.setCode(tour.getCode());
            tourMapper.updateById(updateTour);
        }
        return tour;
    }

    /**
     * 获取行程完整详情（包含套餐、批次等信息）
     */
    public TourDetailDTO getTourDetail(Long id) {
        Tour tour = tourMapper.selectById(id);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }

        TourDetailDTO dto = new TourDetailDTO();

        // 基本信息
        TourDetailDTO.TourBasicInfo basicInfo = new TourDetailDTO.TourBasicInfo();
        basicInfo.setId(tour.getId());
        basicInfo.setTitle(tour.getTitle());
        basicInfo.setSubtitle(tour.getSubtitle());
        // 使用存储的 code，如果没有则生成一个
        if (tour.getCode() != null && !tour.getCode().isEmpty()) {
            basicInfo.setCode(tour.getCode());
        } else {
            basicInfo.setCode(generateTourCode(tour.getTourType()));
        }
        basicInfo.setDays(tour.getDays());
        basicInfo.setDeparture(getCityName(tour.getCity()));
        basicInfo.setEnrolledCount(tour.getEnrolledCount());
        // 使用实体的 notice 字段，如果没有则使用默认值
        String notice = tour.getNotice();
        if (notice == null || notice.isEmpty()) {
            notice = "周边游提前1天，国内游提前3天，出境游提前3-5天，APP和短信群发出团通知";
        }
        basicInfo.setNotice(notice);
        dto.setTour(basicInfo);

        // 标签列表
        dto.setTags(parseTags(tour.getTags()));

        // 产品特色（从feature字段解析）
        dto.setFeatures(parseFeatures(tour.getFeature()));

        // 供应商信息
        TourDetailDTO.SupplierInfo supplier = new TourDetailDTO.SupplierInfo();
        supplier.setName("重庆侠客行国际旅行社有限公司");
        dto.setSupplier(supplier);

        // 退订政策
        TourDetailDTO.RefundPolicy refundPolicy = new TourDetailDTO.RefundPolicy();
        refundPolicy.setSupport("支持退款");
        refundPolicy.setSpecial("特殊原因退订保障");
        dto.setRefundPolicy(refundPolicy);

        // 行程套餐
        List<TourPackage> tourPackages = getTourPackages(id);
        dto.setTripPackages(tourPackages.stream().map(pkg -> {
            TourDetailDTO.PackageInfo info = new TourDetailDTO.PackageInfo();
            info.setId(pkg.getId());
            info.setName(pkg.getName());
            info.setAdultPrice(pkg.getAdultPrice());
            info.setChildPrice(pkg.getChildPrice());
            return info;
        }).collect(Collectors.toList()));

        // 批次套餐
        List<BatchPackage> batchPackages = getBatchPackages(id);
        dto.setBatchPackages(batchPackages.stream().map(pkg -> {
            TourDetailDTO.PackageInfo info = new TourDetailDTO.PackageInfo();
            info.setId(pkg.getId());
            info.setName(pkg.getName());
            info.setExtraFeePerPerson(pkg.getExtraFeePerPerson());
            return info;
        }).collect(Collectors.toList()));

        // 出发日期
        List<TourBatch> batches = getTourBatches(id);
        dto.setBatchDates(batches.stream().map(batch -> {
            TourDetailDTO.BatchDateInfo info = new TourDetailDTO.BatchDateInfo();
            info.setDate(batch.getDepartureDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            info.setAdultDateExtraFee(batch.getAdultDateExtraFee() != null ? batch.getAdultDateExtraFee() : BigDecimal.ZERO);
            info.setChildDateExtraFee(batch.getChildDateExtraFee() != null ? batch.getChildDateExtraFee() : BigDecimal.ZERO);
            info.setStatus(batch.getStatus() != null ? batch.getStatus() : "可报名");
            info.setRemaining(batch.getRemaining() != null ? batch.getRemaining() : 0);
            info.setOccupied(batch.getOccupied() != null ? batch.getOccupied() : 0);
            return info;
        }).collect(Collectors.toList()));

        // 图片信息
        TourDetailDTO.ImageInfo imageInfo = new TourDetailDTO.ImageInfo();
        imageInfo.setMain(generateMainImages(tour.getImages()));
        imageInfo.setThumbnails(generateThumbnails(tour.getImages()));
        dto.setImages(imageInfo);

        // 视频信息
        TourDetailDTO.VideoInfo videoInfo = new TourDetailDTO.VideoInfo();
        videoInfo.setUrl(tour.getVideoUrl() != null ? tour.getVideoUrl() : "");
        videoInfo.setPoster(tour.getVideoPoster() != null ? tour.getVideoPoster() : "");
        videoInfo.setEnabled(tour.getVideoEnabled() != null ? tour.getVideoEnabled() : 0);
        dto.setVideo(videoInfo);

        // 可选酒店列表
        List<TourHotel> tourHotels = getTourHotelsForDetail(id);
        dto.setAvailableHotels(tourHotels.stream().map(hotel -> {
            TourDetailDTO.HotelInfo hotelInfo = new TourDetailDTO.HotelInfo();
            hotelInfo.setId(hotel.getId());
            hotelInfo.setAccommodationId(hotel.getAccommodationId());
            hotelInfo.setName(hotel.getName());
            hotelInfo.setType(hotel.getType());
            hotelInfo.setPriceRange(hotel.getPricePerNight() != null ? hotel.getPricePerNight().toString() : "0");
            hotelInfo.setStarLevel(hotel.getStarLevel());
            hotelInfo.setImageUrl(hotel.getImageUrl());
            return hotelInfo;
        }).collect(Collectors.toList()));

        return dto;
    }

    /**
     * 获取行程关联的酒店列表（用于详情）
     */
    private List<TourHotel> getTourHotelsForDetail(Long tourId) {
        LambdaQueryWrapper<TourHotel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourHotel::getTourId, tourId);
        queryWrapper.eq(TourHotel::getEnabled, 1);
        queryWrapper.orderByAsc(TourHotel::getCreateTime);
        return tourHotelMapper.selectList(queryWrapper);
    }

    /**
     * 获取行程套餐
     */
    public List<TourPackage> getTourPackages(Long tourId) {
        LambdaQueryWrapper<TourPackage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourPackage::getTourId, tourId);
        queryWrapper.eq(TourPackage::getStatus, 1);
        queryWrapper.orderByAsc(TourPackage::getSortOrder);
        return tourPackageMapper.selectList(queryWrapper);
    }

    /**
     * 获取批次套餐
     */
    public List<BatchPackage> getBatchPackages(Long tourId) {
        LambdaQueryWrapper<BatchPackage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BatchPackage::getTourId, tourId);
        queryWrapper.eq(BatchPackage::getStatus, 1);
        queryWrapper.orderByAsc(BatchPackage::getSortOrder);
        return batchPackageMapper.selectList(queryWrapper);
    }

    /**
     * 获取出发日期
     */
    public List<TourBatch> getTourBatches(Long tourId) {
        LambdaQueryWrapper<TourBatch> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourBatch::getTourId, tourId);
        // 只获取未来日期
        queryWrapper.ge(TourBatch::getDepartureDate, LocalDate.now());
        queryWrapper.orderByAsc(TourBatch::getDepartureDate);
        return tourBatchMapper.selectList(queryWrapper);
    }

    /**
     * 解析标签
     */
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // 尝试解析JSON数组
            if (tags.startsWith("[")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(tags, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (Exception e) {
            // 解析失败，按逗号分隔
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 解析产品特色
     */
    private List<String> parseFeatures(String feature) {
        if (feature == null || feature.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(feature.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 生成主图列表
     */
    private List<String> generateMainImages(String mainImage) {
        List<String> images = new ArrayList<>();
        // 优先从 tour.images 字段读取多图
        if (mainImage != null && !mainImage.isEmpty()) {
            try {
                // 尝试解析为 JSON 数组
                if (mainImage.startsWith("[")) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<String> parsedImages = mapper.readValue(mainImage, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    if (parsedImages != null && !parsedImages.isEmpty()) {
                        images.addAll(parsedImages);
                        return images;
                    }
                }
            } catch (Exception e) {
                // 解析失败，当作单个图片URL处理
                System.err.println("解析 images JSON 失败: " + e.getMessage());
            }
            // 如果解析失败，当作单个图片URL处理
            images.add(mainImage);
        }
        // 如果没有图片，返回默认图片
        if (images.isEmpty()) {
            images.add("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&h=600&fit=crop");
        }
        return images;
    }

    /**
     * 生成缩略图列表
     */
    private List<String> generateThumbnails(String images) {
        List<String> thumbnails = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            try {
                // 尝试解析为 JSON 数组
                if (images.startsWith("[")) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<String> parsedImages = mapper.readValue(images, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    if (parsedImages != null && !parsedImages.isEmpty()) {
                        thumbnails.addAll(parsedImages);
                        return thumbnails;
                    }
                }
            } catch (Exception e) {
                System.err.println("解析缩略图 JSON 失败: " + e.getMessage());
            }
            // 解析失败，当作单个图片URL处理
            thumbnails.add(images);
        }
        if (thumbnails.isEmpty()) {
            thumbnails.add("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=150&h=100&fit=crop");
        }
        return thumbnails;
    }

    /**
     * 城市代码转名称
     */
    private String getCityName(String cityCode) {
        if (cityCode == null || cityCode.isEmpty()) {
            return "";
        }
        Map<String, String> cityMap = new HashMap<>();
        cityMap.put("chongqing", "重庆");
        cityMap.put("chengdu", "成都");
        cityMap.put("kunming", "昆明");
        cityMap.put("guiyang", "贵阳");
        cityMap.put("sanya", "三亚");
        cityMap.put("beijing", "北京");
        cityMap.put("shanghai", "上海");
        cityMap.put("changsha", "长沙");
        cityMap.put("yichang", "宜昌");
        cityMap.put("hangzhou", "杭州");
        cityMap.put("haikou", "海口");
        return cityMap.getOrDefault(cityCode, cityCode);
    }

    /**
     * 新增行程
     */
    @Transactional
    public void addTour(Tour tour) {
        // 设置默认状态为上架
        if (tour.getStatus() == null) {
            tour.setStatus(1);
        }
        // 生成业务编码
        if (tour.getCode() == null || tour.getCode().isEmpty()) {
            tour.setCode(generateTourCode(tour.getTourType()));
        }
        tourMapper.insert(tour);
    }

    /**
     * 生成行程业务编码
     * 格式：类型前缀-序号（ZW-001, GT-001, YL-001）
     * 类型前缀：周边游ZW, 长线游CX, 跟团游GT, 邮轮出行YL
     */
    private String generateTourCode(String tourType) {
        // 类型前缀映射
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("around", "ZW");  // 周边游
        prefixMap.put("long", "CX");    // 长线游
        prefixMap.put("team", "GT");    // 跟团游
        prefixMap.put("cruise", "YL"); // 邮轮出行

        String prefix = prefixMap.getOrDefault(tourType, "QT"); // 默认"其他"
        if (prefix.equals("QT") && tourType != null) {
            // 未知类型也尝试用前两个字母
            prefix = tourType.substring(0, Math.min(2, tourType.length())).toUpperCase();
        }

        // 查询该类型已有的最大序号
        LambdaQueryWrapper<Tour> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(Tour::getCode, prefix + "-");
        queryWrapper.orderByDesc(Tour::getId);
        List<Tour> existingTours = tourMapper.selectList(queryWrapper);

        int maxSeq = 0;
        if (existingTours != null && !existingTours.isEmpty()) {
            for (Tour t : existingTours) {
                String code = t.getCode();
                if (code != null && code.contains("-")) {
                    try {
                        String seqStr = code.substring(code.indexOf("-") + 1);
                        int seq = Integer.parseInt(seqStr);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 生成新编码
        return String.format("%s-%03d", prefix, maxSeq + 1);
    }

    /**
     * 更新行程信息
     */
    @Transactional
    public void updateTour(Tour tour) {
        Tour existTour = tourMapper.selectById(tour.getId());
        if (existTour == null) {
            throw new ServiceException("行程不存在");
        }
        tourMapper.updateById(tour);
    }

    /**
     * 删除行程（同时删除关联的套餐、批次等数据）
     */
    @Transactional
    public void deleteTour(Long id) {
        Tour tour = tourMapper.selectById(id);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }
        // 先删除关联的套餐、批次数据
        LambdaQueryWrapper<org.example.springboot.entity.TourPackage> pkgWrapper = new LambdaQueryWrapper<>();
        pkgWrapper.eq(org.example.springboot.entity.TourPackage::getTourId, id);
        tourPackageMapper.delete(pkgWrapper);

        LambdaQueryWrapper<org.example.springboot.entity.BatchPackage> bpWrapper = new LambdaQueryWrapper<>();
        bpWrapper.eq(org.example.springboot.entity.BatchPackage::getTourId, id);
        batchPackageMapper.delete(bpWrapper);

        LambdaQueryWrapper<org.example.springboot.entity.TourBatch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(org.example.springboot.entity.TourBatch::getTourId, id);
        tourBatchMapper.delete(batchWrapper);

        // 最后删除行程本身
        tourMapper.deleteById(id);
    }

    /**
     * 更新行程状态（上架/下架）
     */
    @Transactional
    public void updateTourStatus(Long id, Integer status) {
        Tour tour = tourMapper.selectById(id);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }
        tour.setStatus(status);
        tourMapper.updateById(tour);
    }

    /**
     * 更新行程图片
     */
    @Transactional
    public void updateTourImages(Long id, List<String> images) {
        System.out.println("=== TourService.updateTourImages === id=" + id + " images=" + images);
        Tour tour = tourMapper.selectById(id);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }
        // 存储所有图片到 images 字段（JSON数组）
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            tour.setImages(mapper.writeValueAsString(images));
        } catch (Exception e) {
            System.err.println("序列化图片失败: " + e.getMessage());
        }
        // 同时更新 mainImage 为第一张图片
        if (images != null && !images.isEmpty()) {
            tour.setMainImage(images.get(0));
            System.out.println("设置 mainImage=" + images.get(0));
        } else {
            tour.setMainImage(null);
            System.out.println("清空 mainImage");
        }
        tourMapper.updateById(tour);
        System.out.println("数据库更新完成");
    }

    /**
     * 更新行程视频
     */
    @Transactional
    public void updateTourVideo(Long id, String videoUrl, String videoPoster, Integer videoEnabled) {
        Tour existTour = tourMapper.selectById(id);
        if (existTour == null) {
            throw new ServiceException("行程不存在");
        }
        // 使用 UpdateWrapper 确保更新所有字段（包括0值）
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Tour> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("id", id);
        wrapper.set("video_url", videoUrl);
        wrapper.set("video_poster", videoPoster);
        wrapper.set("video_enabled", videoEnabled != null ? videoEnabled : 0);
        tourMapper.update(null, wrapper);
    }

    /**
     * 获取所有上架的行程
     */
    public List<Tour> getActiveTours() {
        List<Tour> tours = tourMapper.selectList(
            new LambdaQueryWrapper<Tour>()
                .eq(Tour::getStatus, 1)
                .orderByDesc(Tour::getCreateTime)
        );
        // 计算每个行程的实际最低价
        for (Tour tour : tours) {
            BigDecimal minPrice = calculateMinPrice(tour.getId());
            if (minPrice != null) {
                tour.setMinPrice(minPrice);
            }
        }
        return tours;
    }

    /**
     * 根据景点名称和地点推荐相关行程
     * @param scenicName 景点名称
     * @param location 景点地点
     * @param limit 返回数量限制
     * @return 推荐的行程列表
     */
    public List<Tour> getRecommendedToursByScenic(String scenicName, String location, Integer limit) {
        LambdaQueryWrapper<Tour> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询上架的行程
        queryWrapper.eq(Tour::getStatus, 1);

        // 构建模糊匹配条件
        List<String> searchKeywords = new ArrayList<>();

        if (scenicName != null && !scenicName.isEmpty()) {
            searchKeywords.add(scenicName);
        }

        if (location != null && !location.isEmpty()) {
            // 从地点中提取可能的关键词（如城市名）
            String[] locationParts = location.split("[,，\\s]");
            for (String part : locationParts) {
                part = part.trim();
                if (part.length() >= 2) {
                    // 过滤掉太短的部分和具体地址
                    if (!isFilteredKeyword(part)) {
                        searchKeywords.add(part);
                    }
                }
            }
        }

        // 如果有关键词，构建 OR 条件
        if (!searchKeywords.isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (int i = 0; i < searchKeywords.size(); i++) {
                    String keyword = searchKeywords.get(i);
                    if (i == 0) {
                        wrapper.and(q -> q
                            .like(Tour::getTitle, keyword)
                            .or()
                            .like(Tour::getDestination, keyword));
                    } else {
                        wrapper.or(q -> q
                            .like(Tour::getTitle, keyword)
                            .or()
                            .like(Tour::getDestination, keyword));
                    }
                }
            });
        }

        // 按相关性排序（优先按标题匹配，然后按目的地匹配）
        queryWrapper.orderByDesc(Tour::getCreateTime);

        // 限制返回数量
        queryWrapper.last("LIMIT " + (limit != null ? limit : 6));

        List<Tour> tours = tourMapper.selectList(queryWrapper);

        // 计算每个行程的实际最低价
        for (Tour tour : tours) {
            BigDecimal minPrice = calculateMinPrice(tour.getId());
            if (minPrice != null) {
                tour.setMinPrice(minPrice);
            }
        }
        return tours;
    }

    /**
     * 判断是否为需要过滤的关键词
     */
    private boolean isFilteredKeyword(String keyword) {
        // 需要过滤的关键词列表
        String[] filteredKeywords = {
            "路", "街", "号", "弄", "号", "村", "镇", "县",
            "市辖区", "区", "开发区", "度假区", "景区", "公园",
            "大道", "广场", "大厦", "酒店", "宾馆", "客栈"
        };
        for (String filtered : filteredKeywords) {
            if (keyword.endsWith(filtered) || keyword.contains(filtered)) {
                return true;
            }
        }
        // 过滤纯数字
        if (keyword.matches("\\d+")) {
            return true;
        }
        return false;
    }

    // ==================== 首页推荐相关方法 ====================

    /**
     * 获取精选行程
     * 如果管理员设置了推荐，返回设置的行程；否则按默认规则返回
     * 默认规则：返回最近创建的6个上架行程
     */
    public List<Tour> getFeaturedTours() {
        // 查询管理员设置的精选行程
        LambdaQueryWrapper<HomeRecommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HomeRecommend::getType, "featured");
        queryWrapper.orderByAsc(HomeRecommend::getSortOrder);
        List<HomeRecommend> recommends = homeRecommendMapper.selectList(queryWrapper);

        List<Tour> tours = new ArrayList<>();
        
        if (recommends != null && !recommends.isEmpty()) {
            // 有设置的推荐，按设置的顺序返回
            for (HomeRecommend recommend : recommends) {
                Tour tour = tourMapper.selectById(recommend.getTourId());
                if (tour != null && tour.getStatus() != null && tour.getStatus() == 1) {
                    // 计算最低价
                    BigDecimal minPrice = calculateMinPrice(tour.getId());
                    if (minPrice != null) {
                        tour.setMinPrice(minPrice);
                    }
                    tours.add(tour);
                }
            }
        } else {
            // 没有设置，返回默认推荐的行程（最近创建的6个上架行程）
            tours = getDefaultFeaturedTours();
        }
        
        return tours;
    }

    /**
     * 获取更多推荐行程
     * 如果管理员设置了推荐，返回设置的行程；否则按默认规则返回
     * 默认规则：返回除精选行程外的最近创建的行程
     */
    public List<Tour> getMoreTours() {
        // 查询管理员设置的更多推荐行程
        LambdaQueryWrapper<HomeRecommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HomeRecommend::getType, "more");
        queryWrapper.orderByAsc(HomeRecommend::getSortOrder);
        List<HomeRecommend> recommends = homeRecommendMapper.selectList(queryWrapper);

        List<Tour> tours = new ArrayList<>();
        
        if (recommends != null && !recommends.isEmpty()) {
            // 有设置的推荐，按设置的顺序返回
            for (HomeRecommend recommend : recommends) {
                Tour tour = tourMapper.selectById(recommend.getTourId());
                if (tour != null && tour.getStatus() != null && tour.getStatus() == 1) {
                    // 计算最低价
                    BigDecimal minPrice = calculateMinPrice(tour.getId());
                    if (minPrice != null) {
                        tour.setMinPrice(minPrice);
                    }
                    tours.add(tour);
                }
            }
        } else {
            // 没有设置，返回默认推荐（除精选外的最近行程）
            tours = getDefaultMoreTours();
        }
        
        return tours;
    }

    /**
     * 获取默认精选行程（最近创建的6个上架行程）
     */
    private List<Tour> getDefaultFeaturedTours() {
        List<Tour> tours = tourMapper.selectList(
            new LambdaQueryWrapper<Tour>()
                .eq(Tour::getStatus, 1)
                .orderByDesc(Tour::getCreateTime)
                .last("LIMIT 6")
        );
        
        for (Tour tour : tours) {
            BigDecimal minPrice = calculateMinPrice(tour.getId());
            if (minPrice != null) {
                tour.setMinPrice(minPrice);
            }
        }
        return tours;
    }

    /**
     * 获取默认更多推荐行程（精选行程之后的最近行程）
     */
    private List<Tour> getDefaultMoreTours() {
        // 获取已有的精选行程ID
        Set<Long> featuredIds = new HashSet<>();
        LambdaQueryWrapper<HomeRecommend> featuredWrapper = new LambdaQueryWrapper<>();
        featuredWrapper.eq(HomeRecommend::getType, "featured");
        List<HomeRecommend> featuredRecommends = homeRecommendMapper.selectList(featuredWrapper);
        if (featuredRecommends != null) {
            for (HomeRecommend r : featuredRecommends) {
                featuredIds.add(r.getTourId());
            }
        }
        
        // 如果没有精选设置，排除默认精选的6个
        if (featuredIds.isEmpty()) {
            List<Tour> allTours = tourMapper.selectList(
                new LambdaQueryWrapper<Tour>()
                    .eq(Tour::getStatus, 1)
                    .orderByDesc(Tour::getCreateTime)
            );
            
            List<Tour> result = new ArrayList<>();
            for (Tour tour : allTours) {
                if (result.size() >= 10) break;
                if (!featuredIds.contains(tour.getId())) {
                    featuredIds.add(tour.getId()); // 加入已排除集合
                    BigDecimal minPrice = calculateMinPrice(tour.getId());
                    if (minPrice != null) {
                        tour.setMinPrice(minPrice);
                    }
                    result.add(tour);
                }
            }
            return result;
        }
        
        // 排除已有的精选行程，返回其他行程
        LambdaQueryWrapper<Tour> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tour::getStatus, 1);
        queryWrapper.notIn(!featuredIds.isEmpty(), Tour::getId, featuredIds);
        queryWrapper.orderByDesc(Tour::getCreateTime);
        queryWrapper.last("LIMIT 10");
        
        List<Tour> tours = tourMapper.selectList(queryWrapper);
        for (Tour tour : tours) {
            BigDecimal minPrice = calculateMinPrice(tour.getId());
            if (minPrice != null) {
                tour.setMinPrice(minPrice);
            }
        }
        return tours;
    }

    /**
     * 获取所有推荐列表（返回DTO，包含行程详情）
     */
    public List<HomeRecommendDTO> getAllRecommendsDTO() {
        List<HomeRecommend> recommends = homeRecommendMapper.selectList(
            new LambdaQueryWrapper<HomeRecommend>()
                .orderByAsc(HomeRecommend::getSortOrder)
        );
        return convertToDTOList(recommends);
    }

    /**
     * 根据类型获取推荐列表（返回DTO，包含行程详情）
     */
    public List<HomeRecommendDTO> getRecommendsByTypeDTO(String type) {
        List<HomeRecommend> recommends = homeRecommendMapper.selectList(
            new LambdaQueryWrapper<HomeRecommend>()
                .eq(HomeRecommend::getType, type)
                .orderByAsc(HomeRecommend::getSortOrder)
        );
        return convertToDTOList(recommends);
    }

    /**
     * 将推荐列表转换为DTO列表
     */
    private List<HomeRecommendDTO> convertToDTOList(List<HomeRecommend> recommends) {
        List<HomeRecommendDTO> dtoList = new ArrayList<>();
        if (recommends == null || recommends.isEmpty()) {
            return dtoList;
        }
        
        for (HomeRecommend recommend : recommends) {
            HomeRecommendDTO dto = new HomeRecommendDTO();
            dto.setId(recommend.getId());
            dto.setType(recommend.getType());
            dto.setTourId(recommend.getTourId());
            dto.setSortOrder(recommend.getSortOrder());
            dto.setCreateTime(recommend.getCreateTime());
            
            // 获取行程详情
            Tour tour = tourMapper.selectById(recommend.getTourId());
            if (tour != null) {
                dto.setTourIdDetail(tour.getId());
                dto.setTourCode(tour.getCode());
                dto.setTitle(tour.getTitle());
                dto.setSubtitle(tour.getSubtitle());
                dto.setMainImage(tour.getMainImage());
                dto.setTourType(tour.getTourType());
                dto.setDays(tour.getDays());
                dto.setStatus(tour.getStatus());
                
                // 计算最低价
                BigDecimal minPrice = calculateMinPrice(tour.getId());
                dto.setMinPrice(minPrice);
            }
            
            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * 获取所有推荐列表（返回原始对象）
     */
    public List<HomeRecommend> getAllRecommends() {
        return homeRecommendMapper.selectList(
            new LambdaQueryWrapper<HomeRecommend>()
                .orderByAsc(HomeRecommend::getSortOrder)
        );
    }

    /**
     * 根据类型获取推荐列表（返回原始对象）
     */
    public List<HomeRecommend> getRecommendsByType(String type) {
        return homeRecommendMapper.selectList(
            new LambdaQueryWrapper<HomeRecommend>()
                .eq(HomeRecommend::getType, type)
                .orderByAsc(HomeRecommend::getSortOrder)
        );
    }

    /**
     * 添加或更新推荐
     */
    @Transactional
    public void saveRecommend(HomeRecommend recommend) {
        // 检查是否已存在相同类型和行程的推荐
        LambdaQueryWrapper<HomeRecommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HomeRecommend::getType, recommend.getType());
        queryWrapper.eq(HomeRecommend::getTourId, recommend.getTourId());
        HomeRecommend exist = homeRecommendMapper.selectOne(queryWrapper);
        
        if (exist != null) {
            // 已存在，更新排序
            exist.setSortOrder(recommend.getSortOrder());
            exist.setUpdateTime(LocalDateTime.now());
            homeRecommendMapper.updateById(exist);
        } else {
            // 新增
            recommend.setCreateTime(LocalDateTime.now());
            recommend.setUpdateTime(LocalDateTime.now());
            homeRecommendMapper.insert(recommend);
        }
    }

    /**
     * 批量保存推荐
     * featured类型：替换模式（先删后插，保留最后一个）
     * more类型：追加模式（保留已有的，追加新的）
     */
    @Transactional
    public void saveRecommends(String type, List<Long> tourIds) {
        if (tourIds == null || tourIds.isEmpty()) {
            return;
        }
        
        // featured类型：替换模式，只保留新添加的
        if ("featured".equals(type)) {
            // 删除同类型的推荐
            LambdaQueryWrapper<HomeRecommend> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(HomeRecommend::getType, type);
            homeRecommendMapper.delete(delWrapper);
            
            // 只插入最后一个（精选行程只保留一个）
            if (!tourIds.isEmpty()) {
                HomeRecommend recommend = new HomeRecommend();
                recommend.setType(type);
                recommend.setTourId(tourIds.get(tourIds.size() - 1));
                recommend.setSortOrder(1);
                recommend.setCreateTime(LocalDateTime.now());
                recommend.setUpdateTime(LocalDateTime.now());
                homeRecommendMapper.insert(recommend);
            }
            return;
        }
        
        // more类型：追加模式
        // 获取当前最大的排序号
        LambdaQueryWrapper<HomeRecommend> maxWrapper = new LambdaQueryWrapper<>();
        maxWrapper.eq(HomeRecommend::getType, type);
        maxWrapper.orderByDesc(HomeRecommend::getSortOrder);
        maxWrapper.last("LIMIT 1");
        HomeRecommend lastRecommend = homeRecommendMapper.selectOne(maxWrapper);
        int maxSort = (lastRecommend != null && lastRecommend.getSortOrder() != null) ? lastRecommend.getSortOrder() : 0;
        
        // 追加新的推荐
        for (Long tourId : tourIds) {
            // 检查是否已存在
            LambdaQueryWrapper<HomeRecommend> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(HomeRecommend::getType, type);
            existWrapper.eq(HomeRecommend::getTourId, tourId);
            HomeRecommend exist = homeRecommendMapper.selectOne(existWrapper);
            
            if (exist == null) {
                HomeRecommend recommend = new HomeRecommend();
                recommend.setType(type);
                recommend.setTourId(tourId);
                recommend.setSortOrder(++maxSort);
                recommend.setCreateTime(LocalDateTime.now());
                recommend.setUpdateTime(LocalDateTime.now());
                homeRecommendMapper.insert(recommend);
            }
        }
    }

    /**
     * 删除推荐
     */
    @Transactional
    public void deleteRecommend(Long id) {
        homeRecommendMapper.deleteById(id);
    }

    /**
     * 更新推荐排序
     */
    @Transactional
    public void updateRecommendSort(List<Number> ids) {
        for (int i = 0; i < ids.size(); i++) {
            HomeRecommend recommend = homeRecommendMapper.selectById(ids.get(i).longValue());
            if (recommend != null) {
                recommend.setSortOrder(i + 1);
                recommend.setUpdateTime(LocalDateTime.now());
                homeRecommendMapper.updateById(recommend);
            }
        }
    }

    /**
     * 清空指定类型的推荐
     */
    @Transactional
    public void clearRecommendsByType(String type) {
        LambdaQueryWrapper<HomeRecommend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HomeRecommend::getType, type);
        homeRecommendMapper.delete(queryWrapper);
    }
}
