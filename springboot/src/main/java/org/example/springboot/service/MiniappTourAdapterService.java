package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.dto.TourProductSourceConfigDTO;
import org.example.springboot.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MiniappTourAdapterService {
    public static final String SOURCE_TYPE = "MINIAPP";
    private static final String TOUR_ID_PREFIX = "miniapp-";
    private static final int REMOTE_PAGE_SIZE = 100;
    private static final int MAX_REMOTE_PAGES = 100;
    private static final long SUMMARY_CACHE_MILLIS = 15_000L;
    private static final long SUMMARY_STALE_MILLIS = 10 * 60_000L;
    private static final long FAILURE_BACKOFF_MILLIS = 5_000L;
    private static final Pattern RELATIVE_UPLOAD = Pattern.compile(
            "(^|[\\s\\\"'(<=>])(/?(?:api/)?uploads/[^\\\"'<>\\s)\\]}]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(MiniappTourAdapterService.class);

    @Resource
    private TourProductSourceConfigService configService;

    @Resource
    private MiniappInventoryService miniappInventoryService;

    @Resource
    private TourSourceDisplayConfigService displayConfigService;

    private final RestTemplate restTemplate;
    private volatile List<Map<String, Object>> summaryCache = List.of();
    private volatile String summaryCacheApiBaseUrl = "";
    private volatile long summaryCacheExpiresAt = 0L;
    private volatile long summaryCacheUpdatedAt = 0L;
    private volatile long summaryFailureBackoffUntil = 0L;

    public MiniappTourAdapterService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public Page<Map<String, Object>> getTourPage(
            String keyword,
            String tourType,
            String city,
            String destination,
            String days,
            String month,
            String priceRange,
            String searchMode,
            String intentDestination,
            String matchMode,
            String sortType,
            Integer currentPage,
            Integer pageSize) {
        RemoteQuery query = new RemoteQuery(keyword, tourType, city, destination, days, month,
                priceRange, searchMode, intentDestination, matchMode);
        List<Map<String, Object>> tours = filterTours(loadAdaptedSummaries(), query, null);
        sortTours(tours, sortType);

        long current = currentPage == null || currentPage < 1 ? 1 : currentPage;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        long total = tours.size();
        int from = (int) Math.min((current - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        Page<Map<String, Object>> page = new Page<>(current, size, total);
        page.setRecords(new ArrayList<>(tours.subList(from, to)));
        return page;
    }

    public Page<Map<String, Object>> getManageTourPage(
            String keyword,
            String tourType,
            String city,
            String destination,
            Integer currentPage,
            Integer pageSize) {
        RemoteQuery query = new RemoteQuery(keyword, tourType, city, destination,
                "", "", "", "", "", "");
        List<Map<String, Object>> tours = filterTours(loadAdaptedSummaries(true), query, null);
        sortTours(tours, "default");

        long current = currentPage == null || currentPage < 1 ? 1 : currentPage;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        long total = tours.size();
        int from = (int) Math.min((current - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        Page<Map<String, Object>> page = new Page<>(current, size, total);
        page.setRecords(new ArrayList<>(tours.subList(from, to)));
        return page;
    }

    public Map<String, List<Map<String, Object>>> getTourFilters(
            String keyword,
            String tourType,
            String city,
            String destination,
            String days,
            String month,
            String priceRange,
            String searchMode,
            String intentDestination,
            String matchMode) {
        List<Map<String, Object>> source = loadAdaptedSummaries();
        RemoteQuery query = new RemoteQuery(keyword, tourType, city, destination, days, month,
                priceRange, searchMode, intentDestination, matchMode);
        Map<String, List<Map<String, Object>>> filters = new LinkedHashMap<>();
        filters.put("tourTypes", countOptions(filterTours(source, query, "tourType"), item -> text(item.get("tourType"))));
        filters.put("cities", countOptions(filterTours(source, query, "city"), item -> text(item.get("city"))));
        filters.put("destinations", countOptions(filterTours(source, query, "destination"), item -> text(item.get("destination"))));
        filters.put("daysList", countOptions(filterTours(source, query, "days"), item -> daysRange(integer(item.get("days")))));
        filters.put("months", countOptions(filterTours(source, query, "month"), item -> text(item.get("month"))));
        filters.put("priceRanges", countOptions(filterTours(source, query, "priceRange"), item ->
                "inquiry".equalsIgnoreCase(text(item.get("pricingMode")))
                        ? "" : priceRange(decimal(item.get("minPrice")))));
        return filters;
    }

    public List<Map<String, String>> getHotKeywords(int limit) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Object> tour : loadAdaptedSummaries()) {
            addKeyword(counts, text(tour.get("destination")));
            addKeyword(counts, text(tour.get("city")));
            for (String tag : stringList(tour.get("tags"))) {
                addKeyword(counts, tag);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(entry -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("value", entry.getKey());
                    item.put("label", entry.getKey());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getActiveTours() {
        return loadAdaptedSummaries();
    }

    public List<Map<String, Object>> getFeaturedTours(int limit) {
        List<Map<String, Object>> tours = loadAdaptedSummaries();
        return new ArrayList<>(tours.subList(0, Math.min(Math.max(limit, 0), tours.size())));
    }

    public List<Map<String, Object>> getMoreTours(int offset, int limit) {
        List<Map<String, Object>> tours = loadAdaptedSummaries();
        int from = Math.min(Math.max(offset, 0), tours.size());
        int to = Math.min(from + Math.max(limit, 0), tours.size());
        return new ArrayList<>(tours.subList(from, to));
    }

    public List<Map<String, Object>> getRecommendedTours(String scenicName, String location, int limit) {
        String keyword = String.join(" ", nonBlankValues(scenicName, location));
        List<Map<String, Object>> tours = loadAdaptedSummaries();
        if (!keyword.isBlank()) {
            List<Map<String, Object>> matched = tours.stream()
                    .filter(item -> matchesAnyToken(item, keyword))
                    .collect(Collectors.toList());
            if (!matched.isEmpty()) {
                tours = matched;
            }
        }
        return new ArrayList<>(tours.subList(0, Math.min(Math.max(limit, 1), tours.size())));
    }

    public Map<String, Object> getTourDetail(String encodedId) {
        String remoteId = decodeTourId(encodedId);
        if (!displayConfigService.isVisible(SOURCE_TYPE, remoteId)) {
            throw new ServiceException("行程不存在或已停止展示");
        }
        TourProductSourceConfigDTO config = configService.getConfig();
        Map<String, Object> remote = getObjectData(buildUri(config.getMiniappApiBaseUrl(), "/tours/detail", Map.of("id", remoteId)));
        normalizeRemoteAssets(remote, config.getMiniappApiBaseUrl());
        Map<String, Object> detail = adaptDetail(remote, remoteId, config);
        miniappInventoryService.applyLocalAllocations(detail);
        return detail;
    }

    public String encodeTourId(String remoteId) {
        if (remoteId == null || remoteId.isBlank()) {
            throw new ServiceException("无效的小程序商品编号");
        }
        return encodeRemoteTourId(remoteId.trim());
    }

    public boolean isMiniappTourId(String id) {
        return id != null && id.startsWith(TOUR_ID_PREFIX);
    }

    public Map<String, Object> checkConnection() {
        return checkConnection(null);
    }

    public Map<String, Object> checkConnection(TourProductSourceConfigDTO candidate) {
        TourProductSourceConfigDTO config = candidate == null
                ? configService.getConfig()
                : configService.prepareForConnectionCheck(candidate);
        if (config.getMiniappApiBaseUrl() == null || config.getMiniappApiBaseUrl().isBlank()) {
            throw new ServiceException("请先填写小程序 API 地址");
        }
        List<Map<String, Object>> tours = fetchAllRemoteSummaries(config);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("connected", true);
        result.put("productCount", tours.size());
        result.put("sampleTitle", tours.isEmpty() ? "" : text(tours.get(0).get("title")));
        result.put("apiBaseUrl", config.getMiniappApiBaseUrl());
        return result;
    }

    public synchronized void invalidateSummaryCache() {
        summaryCache = List.of();
        summaryCacheApiBaseUrl = "";
        summaryCacheExpiresAt = 0L;
        summaryCacheUpdatedAt = 0L;
        summaryFailureBackoffUntil = 0L;
    }

    private List<Map<String, Object>> loadAdaptedSummaries() {
        return loadAdaptedSummaries(false);
    }

    private List<Map<String, Object>> loadAdaptedSummaries(boolean includeHidden) {
        return displayConfigService.apply(SOURCE_TYPE, loadCachedAdaptedSummaries(), includeHidden);
    }

    private synchronized List<Map<String, Object>> loadCachedAdaptedSummaries() {
        TourProductSourceConfigDTO config = configService.getConfig();
        long now = System.currentTimeMillis();
        boolean sameSource = config.getMiniappApiBaseUrl().equals(summaryCacheApiBaseUrl);
        if (sameSource && now < summaryCacheExpiresAt) {
            return new ArrayList<>(summaryCache);
        }
        if (sameSource && !summaryCache.isEmpty() && now < summaryFailureBackoffUntil) {
            return new ArrayList<>(summaryCache);
        }
        try {
            List<Map<String, Object>> remoteTours = fetchAllRemoteSummaries(config);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> remote : remoteTours) {
                normalizeRemoteAssets(remote, config.getMiniappApiBaseUrl());
                result.add(adaptSummary(remote));
            }
            summaryCache = result;
            summaryCacheApiBaseUrl = config.getMiniappApiBaseUrl();
            summaryCacheExpiresAt = now + SUMMARY_CACHE_MILLIS;
            summaryCacheUpdatedAt = now;
            summaryFailureBackoffUntil = 0L;
            return new ArrayList<>(result);
        } catch (ServiceException ex) {
            if (sameSource && !summaryCache.isEmpty() && now - summaryCacheUpdatedAt <= SUMMARY_STALE_MILLIS) {
                summaryFailureBackoffUntil = now + FAILURE_BACKOFF_MILLIS;
                LOGGER.warn("Miniapp tour catalog refresh failed, serving recent cached data: {}", ex.getMessage());
                return new ArrayList<>(summaryCache);
            }
            throw ex;
        }
    }

    private List<Map<String, Object>> fetchAllRemoteSummaries(TourProductSourceConfigDTO config) {
        String apiBaseUrl = config.getMiniappApiBaseUrl();
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            throw new ServiceException("尚未配置小程序 API 地址");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        long total = Long.MAX_VALUE;
        int page = 1;
        while (result.size() < total && page <= MAX_REMOTE_PAGES) {
            Map<String, Object> data = getObjectData(buildUri(apiBaseUrl, "/tours", Map.of(
                    "page", page,
                    "pageSize", REMOTE_PAGE_SIZE
            )));
            List<Map<String, Object>> records = mapList(data.get("list"));
            total = longValue(data.get("total"), records.size());
            result.addAll(records);
            if (records.size() < REMOTE_PAGE_SIZE) {
                break;
            }
            page++;
        }
        if (page > MAX_REMOTE_PAGES && result.size() < total) {
            LOGGER.warn("Miniapp tour list exceeded adapter page limit: fetched={}, total={}", result.size(), total);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getObjectData(URI uri) {
        try {
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response == null) {
                throw new ServiceException("小程序商品接口未返回数据");
            }
            if (integer(response.get("code")) != 0) {
                throw new ServiceException(defaultText(response.get("message"), "小程序商品接口请求失败"));
            }
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?>)) {
                throw new ServiceException("小程序商品接口返回格式不正确");
            }
            return (Map<String, Object>) data;
        } catch (ServiceException ex) {
            throw ex;
        } catch (RestClientException | IllegalArgumentException ex) {
            LOGGER.warn("Request miniapp tour API failed: {}", uri, ex);
            throw new ServiceException("无法连接小程序商品接口，请检查地址和服务状态");
        }
    }

    private URI buildUri(String baseUrl, String path, Map<String, ?> params) {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(normalizedBaseUrl).path(path);
        params.forEach((key, value) -> {
            if (value != null && !text(value).isBlank()) {
                builder.queryParam(key, value);
            }
        });
        return builder.build().encode().toUri();
    }

    private Map<String, Object> adaptSummary(Map<String, Object> remote) {
        Map<String, Object> item = new LinkedHashMap<>(remote);
        String remoteId = text(remote.get("id"));
        String nextScheduleDate = text(remote.get("nextScheduleDate"));
        Map<String, Object> promotion = map(remote.get("promotion"));
        Map<String, Object> cruiseHomeSummary = map(remote.get("cruiseHomeSummary"));
        List<String> tags = displayTags(remote);
        BigDecimal price = decimal(remote.get("price"));
        BigDecimal originalPrice = discountOriginal(remote.get("originalPrice"), price);
        String pricingMode = "inquiry".equalsIgnoreCase(text(remote.get("pricingMode"))) ? "inquiry" : "fixed";

        item.put("id", encodeRemoteTourId(remoteId));
        item.put("sourceType", SOURCE_TYPE);
        item.put("sourceId", remoteId);
        item.put("code", "MINI-" + remoteId);
        item.put("mainImage", firstNonBlank(cruiseHomeSummary.get("cover"), remote.get("cover"), first(stringList(remote.get("images")))));
        item.put("tag", firstNonBlank(remote.get("coverBadge"), remote.get("categoryLabel")));
        item.put("tourType", resolveTourType(remote));
        item.put("city", remote.get("departureCity"));
        item.put("destination", remote.get("destination"));
        item.put("days", integerOrDefault(remote.get("days"), 1));
        item.put("nights", Math.max(0, integer(remote.get("nights"))));
        item.put("month", monthOf(nextScheduleDate));
        item.put("minPrice", price);
        item.put("minOriginalPrice", originalPrice);
        item.put("minDiscountLabel", discountLabel(promotion, originalPrice, price));
        item.put("minSavedAmount", savedAmount(originalPrice, price));
        item.put("pricingMode", pricingMode);
        item.put("priceText", "inquiry".equals(pricingMode)
                ? defaultText(remote.get("priceText"), "客服咨询") : "");
        item.put("priceUnit", defaultText(remote.get("priceUnit"), "person"));
        item.put("starRating", decimal(remote.get("score")));
        item.put("commentCount", integer(remote.get("commentCount")));
        item.put("recommendDate", nextScheduleDate);
        item.put("moreDates", text(cruiseHomeSummary.get("departureDateText")));
        item.put("feature", resolveFeature(remote));
        item.put("tags", tags);
        item.put("enrolledCount", firstInteger(remote.get("displayRegistrationCount"), remote.get("saleCount")));
        item.put("actualRegistrationCount", integer(remote.get("actualRegistrationCount")));
        item.put("registrationDisplayMode", remote.get("registrationDisplayMode"));
        item.put("categoryLabel", remote.get("categoryLabel"));
        item.put("sortOrder", integer(remote.get("sortOrder")));
        item.put("status", 1);
        return item;
    }

    private Map<String, Object> adaptDetail(
            Map<String, Object> remote,
            String remoteTourId,
            TourProductSourceConfigDTO config) {
        Map<String, Object> summary = adaptSummary(remote);
        Map<String, Object> cruiseBooking = map(remote.get("cruiseBooking"));
        CruiseProductAdaptation cruiseProduct = adaptCruiseProduct(cruiseBooking, remote);

        List<Map<String, Object>> packages;
        List<Map<String, Object>> schedules;
        List<Map<String, Object>> packagePrices;
        List<Map<String, Object>> addons;
        List<Map<String, Object>> addonPrices;
        if (cruiseProduct != null) {
            packages = cruiseProduct.packages();
            schedules = cruiseProduct.schedules();
            packagePrices = cruiseProduct.packagePrices();
            addons = List.of();
            addonPrices = List.of();
        } else {
            List<Map<String, Object>> remotePackages = mapList(remote.get("packages"));
            if (remotePackages.isEmpty()) {
                Map<String, Object> defaultPackage = new LinkedHashMap<>();
                defaultPackage.put("id", "default");
                defaultPackage.put("_synthetic", true);
                defaultPackage.put("name", "标准套餐");
                defaultPackage.put("price", remote.get("price"));
                defaultPackage.put("originalPrice", remote.get("originalPrice"));
                defaultPackage.put("description", remote.get("subtitle"));
                remotePackages.add(defaultPackage);
            }
            List<Map<String, Object>> remoteSchedules = mapList(remote.get("schedules"));
            List<Map<String, Object>> remoteAddons = mapList(remote.get("addonPackages"));
            IdRegistry packageIds = new IdRegistry(remotePackages);
            IdRegistry scheduleIds = new IdRegistry(remoteSchedules);
            IdRegistry addonIds = new IdRegistry(remoteAddons);

            packages = adaptPackages(remotePackages, packageIds, remote);
            schedules = adaptSchedules(remoteSchedules, scheduleIds, packageIds, addonIds);
            Map<Long, BigDecimal> packageBasePrices = packages.stream().collect(Collectors.toMap(
                    item -> longValue(item.get("id"), 0L),
                    item -> decimal(item.get("adultPrice")),
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
            packagePrices = adaptPackagePriceItems(
                    mapList(remote.get("packagePriceItems")), packageIds, scheduleIds, packageBasePrices);
            if (packagePrices.isEmpty()) {
                packagePrices = synthesizeSchedulePackagePrices(
                        remotePackages, remoteSchedules, packageIds, scheduleIds, packageBasePrices);
            }
            addons = adaptAddons(remoteAddons, addonIds);
            addonPrices = adaptAddonPriceItems(
                    mapList(remote.get("addonPriceItems")), addonIds, scheduleIds, packageIds);
        }

        Map<String, Object> tour = new LinkedHashMap<>();
        tour.put("id", summary.get("id"));
        tour.put("sourceType", SOURCE_TYPE);
        tour.put("sourceId", remoteTourId);
        tour.put("title", remote.get("title"));
        tour.put("subtitle", remote.get("subtitle"));
        tour.put("code", summary.get("code"));
        tour.put("days", summary.get("days"));
        tour.put("nights", summary.get("nights"));
        tour.put("minPrice", summary.get("minPrice"));
        tour.put("minOriginalPrice", summary.get("minOriginalPrice"));
        tour.put("minDiscountLabel", summary.get("minDiscountLabel"));
        tour.put("minSavedAmount", summary.get("minSavedAmount"));
        tour.put("departure", remote.get("departureCity"));
        tour.put("destination", remote.get("destination"));
        tour.put("tourType", summary.get("tourType"));
        tour.put("categoryLabel", remote.get("categoryLabel"));
        tour.put("pricingMode", summary.get("pricingMode"));
        tour.put("priceText", summary.get("priceText"));
        tour.put("priceUnit", summary.get("priceUnit"));
        tour.put("isCruise", !cruiseBooking.isEmpty());
        tour.put("enrolledCount", summary.get("enrolledCount"));
        tour.put("commentCount", summary.get("commentCount"));
        tour.put("score", summary.get("starRating"));
        tour.put("recommendDate", summary.get("recommendDate"));
        tour.put("moreDates", summary.get("moreDates"));
        tour.put("difficulty", remote.get("difficulty"));
        tour.put("groupSizeText", remote.get("groupSizeText"));
        tour.put("ageRange", remote.get("ageRange"));
        tour.put("suitableFor", stringList(remote.get("suitableFor")));
        tour.put("travelerLimit", integer(remote.get("travelerLimit")));
        tour.put("notice", joinNotices(remote));
        tour.put("detailContent", buildDetailContent(remote));

        Map<String, Object> mediaInfo = adaptMedia(remote);
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) mediaInfo.get("images");
        Map<String, Object> imageInfo = new LinkedHashMap<>();
        imageInfo.put("main", images);
        imageInfo.put("thumbnails", new ArrayList<>(images));

        List<String> introductions = productIntroductions(remote);
        List<String> features = mergeStringLists(
                remote.get("highlights"), remote.get("bookingFeatures"), remote.get("recommendedReason"));
        for (Map<String, Object> guarantee : mapList(remote.get("serviceGuarantees"))) {
            String title = text(guarantee.get("title")).trim();
            if (!title.isBlank() && !features.contains(title)) features.add(title);
        }
        Map<String, Object> refundPolicy = new LinkedHashMap<>();
        List<String> refundItems = stringList(remote.get("refundPolicy"));
        refundPolicy.put("support", refundItems.isEmpty() ? "按商品规则退订" : refundItems.get(0));
        refundPolicy.put("special", refundItems.size() > 1 ? refundItems.get(1) : "");
        refundPolicy.put("content", listSection("退订政策", refundItems));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceType", SOURCE_TYPE);
        result.put("sourceId", remoteTourId);
        result.put("tour", tour);
        result.put("tags", displayTags(remote));
        result.put("features", features);
        result.put("featureText", featureText(introductions, features));
        result.put("supplier", Map.of("name", "小程序统一商品"));
        result.put("refundPolicy", refundPolicy);
        result.put("tripPackages", packages);
        result.put("packagePriceItems", packagePrices);
        result.put("batchPackages", addons);
        result.put("addonPriceItems", addonPrices);
        result.put("batchDates", schedules);
        result.put("images", imageInfo);
        result.put("video", mediaInfo.get("video"));
        result.put("availableHotels", List.of());
        result.put("stats", mapList(remote.get("stats")));
        result.put("serviceGuarantees", mapList(remote.get("serviceGuarantees")));
        result.put("reviews", mapList(remote.get("reviews")));
        result.put("ticketTypes", mapList(remote.get("ticketTypes")));
        result.put("cruiseBooking", cruiseBooking);
        result.put("bookingPopupNotice", map(remote.get("bookingPopupNotice")));
        result.put("orderConfirmation", orderConfirmation(remote));
        result.put("miniappFields", remote);
        return result;
    }

    private CruiseProductAdaptation adaptCruiseProduct(
            Map<String, Object> cruiseBooking,
            Map<String, Object> tour) {
        List<Map<String, Object>> routes = mapList(cruiseBooking.get("routes"));
        if (routes.isEmpty()) {
            return null;
        }

        Map<String, Map<String, Object>> packageSources = new LinkedHashMap<>();
        List<Map<String, Object>> scheduleSources = new ArrayList<>();
        for (Map<String, Object> route : routes) {
            for (Map<String, Object> schedule : mapList(route.get("schedules"))) {
                String scheduleId = text(schedule.get("id")).trim();
                String date = text(schedule.get("date")).trim();
                if (scheduleId.isBlank() || date.isBlank()) continue;

                for (Map<String, Object> cabin : mapList(schedule.get("cabins"))) {
                    String cabinId = firstNonBlank(cabin.get("id"), cabin.get("cabinId"));
                    String scheduleCabinId = firstNonBlank(cabin.get("scheduleCabinId"), cabinId);
                    if (cabinId.isBlank() || scheduleCabinId.isBlank()) continue;

                    Map<String, Object> packageSource = packageSources.computeIfAbsent(cabinId, key -> {
                        Map<String, Object> item = new LinkedHashMap<>(cabin);
                        item.put("id", key);
                        item.put("name", defaultText(cabin.get("name"), "邮轮房型"));
                        item.put("description", cruiseCabinDescription(cabin));
                        return item;
                    });
                    BigDecimal currentBasePrice = positiveDecimal(packageSource.get("price"));
                    BigDecimal cabinPrice = positiveDecimal(cabin.get("price"));
                    if (currentBasePrice == null || (cabinPrice != null && cabinPrice.compareTo(currentBasePrice) < 0)) {
                        packageSource.put("price", cabin.get("price"));
                        packageSource.put("originalPrice", cabin.get("originalPrice"));
                        packageSource.put("promotion", cabin.get("promotion"));
                    }

                    Map<String, Object> scheduleSource = new LinkedHashMap<>();
                    String compositeScheduleId = scheduleId + "::" + scheduleCabinId;
                    scheduleSource.put("id", compositeScheduleId);
                    scheduleSource.put("remoteScheduleId", scheduleId);
                    scheduleSource.put("scheduleCabinId", scheduleCabinId);
                    scheduleSource.put("date", date);
                    scheduleSource.put("endDate", schedule.get("endDate"));
                    scheduleSource.put("status", firstNonNull(cabin.get("status"), schedule.get("status")));
                    int cabinCapacity = Math.max(integerOrDefault(cabin.get("capacity"), 1), 1);
                    int availableRooms = Math.max(firstInteger(cabin.get("availableStock"), schedule.get("availableStock")), 0);
                    int availablePassengerStock = (int) Math.min(
                            (long) availableRooms * cabinCapacity,
                            999999L);
                    scheduleSource.put("stock", availablePassengerStock);
                    scheduleSource.put("availableStock", availablePassengerStock);
                    scheduleSource.put("remoteAvailableRooms", availableRooms);
                    scheduleSource.put("lockedStock", 0);
                    scheduleSource.put("unlimitedStock", firstNonNull(cabin.get("unlimitedStock"), schedule.get("unlimitedStock")));
                    scheduleSource.put("packageIds", List.of(cabinId));
                    scheduleSource.put("_packageSourceId", cabinId);
                    scheduleSource.put("_priceSourceId", scheduleCabinId);
                    scheduleSource.put("_cabin", cabin);
                    scheduleSources.add(scheduleSource);
                }
            }
        }
        if (packageSources.isEmpty() || scheduleSources.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> remotePackages = new ArrayList<>(packageSources.values());
        IdRegistry packageIds = new IdRegistry(remotePackages);
        IdRegistry scheduleIds = new IdRegistry(scheduleSources);
        IdRegistry emptyAddonIds = new IdRegistry(List.of());
        List<Map<String, Object>> packages = adaptPackages(remotePackages, packageIds, tour);
        for (Map<String, Object> item : packages) {
            item.put("childPrice", null);
            item.put("originalChildPrice", null);
            item.put("childDiscountLabel", "");
            item.put("childSavedAmount", BigDecimal.ZERO);
        }
        List<Map<String, Object>> schedules = adaptSchedules(
                scheduleSources, scheduleIds, packageIds, emptyAddonIds);

        Map<Long, BigDecimal> packageBasePrices = packages.stream().collect(Collectors.toMap(
                item -> longValue(item.get("id"), 0L),
                item -> decimal(item.get("adultPrice")),
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<Map<String, Object>> packagePrices = new ArrayList<>();
        long nextPriceId = 1;
        for (Map<String, Object> scheduleSource : scheduleSources) {
            Long packageId = packageIds.idOrNull(scheduleSource.get("_packageSourceId"));
            Long batchId = scheduleIds.idOrNull(scheduleSource.get("id"));
            Map<String, Object> cabin = map(scheduleSource.get("_cabin"));
            if (packageId == null || batchId == null || cabin.isEmpty()) continue;
            Map<String, Object> source = new LinkedHashMap<>(cabin);
            source.put("id", scheduleSource.get("_priceSourceId"));
            source.put("name", defaultText(cabin.get("name"), "房型价格"));
            source.put("adultPrice", cabin.get("price"));
            source.put("originalAdultPrice", cabin.get("originalPrice"));
            source.put("childPrice", null);
            source.put("originalChildPrice", null);
            packagePrices.add(packagePriceItem(
                    nextPriceId++, source, source, packageId, List.of(batchId), packageBasePrices.get(packageId)));
        }
        return new CruiseProductAdaptation(packages, schedules, packagePrices);
    }

    private String cruiseCabinDescription(Map<String, Object> cabin) {
        List<String> parts = new ArrayList<>();
        addLabeledItem(parts, "房型", cabin.get("roomType"));
        addLabeledItem(parts, "面积", cabin.get("area"));
        addLabeledItem(parts, "楼层", cabin.get("floor"));
        addLabeledItem(parts, "可住", cabin.get("capacity"));
        addLabeledItem(parts, "床型", cabin.get("bedType"));
        addLabeledItem(parts, "窗型", cabin.get("windowType"));
        List<String> facilities = stringList(cabin.get("facilities"));
        if (!facilities.isEmpty()) parts.add("设施：" + String.join("、", facilities));
        return String.join("；", parts);
    }

    private List<Map<String, Object>> adaptPackages(
            List<Map<String, Object>> remotePackages,
            IdRegistry packageIds,
            Map<String, Object> tour) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> source : remotePackages) {
            Map<String, Object> promotion = map(source.get("promotion"));
            BigDecimal adultPrice = effectiveFixedPrice(source.get("price"), tour.get("price"));
            BigDecimal childPrice = positiveDecimal(source.get("childPrice"));
            BigDecimal originalAdultPrice = discountOriginal(
                    firstNonNull(source.get("originalPrice"), source.get("originalAdultPrice")), adultPrice);
            BigDecimal originalChildPrice = discountOriginal(source.get("originalChildPrice"), childPrice);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", packageIds.id(source.get("id")));
            item.put("sourceId", bool(source.get("_synthetic")) ? "__default__" : text(source.get("id")));
            item.put("name", defaultText(source.get("name"), "标准套餐"));
            item.put("adultPrice", adultPrice);
            item.put("childPrice", childPrice);
            item.put("originalAdultPrice", originalAdultPrice);
            item.put("originalChildPrice", originalChildPrice);
            item.put("adultDiscountLabel", discountLabel(promotion, originalAdultPrice, adultPrice));
            item.put("childDiscountLabel", discountLabel(promotion, originalChildPrice, childPrice));
            item.put("adultSavedAmount", savedAmount(originalAdultPrice, adultPrice));
            item.put("childSavedAmount", savedAmount(originalChildPrice, childPrice));
            item.put("description", source.get("description"));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> adaptSchedules(
            List<Map<String, Object>> remoteSchedules,
            IdRegistry scheduleIds,
            IdRegistry packageIds,
            IdRegistry addonIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> source : remoteSchedules) {
            String date = text(source.get("date"));
            if (date.isBlank()) {
                continue;
            }
            List<Long> mappedPackageIds = mapRelatedIds(source.get("packageIds"), packageIds);
            if (mappedPackageIds.isEmpty() && !text(source.get("packageId")).isBlank()) {
                Long packageId = packageIds.idOrNull(source.get("packageId"));
                if (packageId != null) {
                    mappedPackageIds.add(packageId);
                }
            }
            if (mappedPackageIds.isEmpty()) {
                mappedPackageIds.addAll(packageIds.allIds());
            }
            List<Long> mappedAddonIds = mapRelatedIds(source.get("addonIds"), addonIds);
            boolean unlimitedStock = bool(source.get("unlimitedStock"));
            int available = unlimitedStock
                    ? 999999
                    : firstInteger(source.get("availableStock"), source.get("stock"));
            int locked = firstInteger(source.get("lockedStock"));
            String status = text(source.get("status"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", scheduleIds.id(source.get("id")));
            item.put("sourceId", text(source.get("id")));
            item.put("date", date);
            item.put("endDate", source.get("endDate"));
            item.put("adultDateExtraFee", BigDecimal.ZERO);
            item.put("childDateExtraFee", BigDecimal.ZERO);
            item.put("status", scheduleStatus(status, available));
            item.put("remaining", unlimitedStock ? 999999 : Math.max(available, 0) + Math.max(locked, 0));
            item.put("occupied", unlimitedStock ? 0 : Math.max(locked, 0));
            item.put("packageIds", mappedPackageIds);
            item.put("addonIds", mappedAddonIds);
            item.put("adultPrice", decimal(source.get("adultPrice")));
            item.put("childPrice", positiveDecimal(source.get("childPrice")));
            item.put("originalAdultPrice", positiveDecimal(source.get("originalAdultPrice")));
            item.put("originalChildPrice", positiveDecimal(source.get("originalChildPrice")));
            item.put("singleRoomDiff", decimal(source.get("singleRoomDiff")));
            item.put("minGroupSize", integer(source.get("minGroupSize")));
            item.put("stock", integer(source.get("stock")));
            item.put("availableStock", Math.max(available, 0));
            item.put("remoteAvailableStock", Math.max(available, 0));
            item.put("remoteStatus", status);
            item.put("lockedStock", Math.max(locked, 0));
            item.put("bookedCount", Math.max(firstInteger(source.get("bookedCount")), 0));
            item.put("unlimitedStock", unlimitedStock);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> adaptPackagePriceItems(
            List<Map<String, Object>> remoteItems,
            IdRegistry packageIds,
            IdRegistry scheduleIds,
            Map<Long, BigDecimal> packageBasePrices) {
        List<Map<String, Object>> result = new ArrayList<>();
        long nextId = 1;
        for (Map<String, Object> source : remoteItems) {
            Long packageId = packageIds.idOrNull(source.get("packageId"));
            if (packageId == null || !enabled(source.get("status"))) {
                continue;
            }
            Map<String, Object> schedulePrices = map(source.get("schedulePrices"));
            if (!schedulePrices.isEmpty()) {
                for (Map.Entry<String, Object> entry : schedulePrices.entrySet()) {
                    Long batchId = scheduleIds.idOrNull(entry.getKey());
                    if (batchId == null) {
                        continue;
                    }
                    result.add(packagePriceItem(nextId++, source, map(entry.getValue()), packageId,
                            List.of(batchId), packageBasePrices.get(packageId)));
                }
            } else {
                List<Long> batchIds = mapRelatedIds(source.get("scheduleIds"), scheduleIds);
                if (batchIds.isEmpty()) batchIds = scheduleIds.allIds();
                result.add(packagePriceItem(nextId++, source, source, packageId,
                        batchIds, packageBasePrices.get(packageId)));
            }
        }
        return result;
    }

    private Map<String, Object> packagePriceItem(
            long id,
            Map<String, Object> source,
            Map<String, Object> price,
            Long packageId,
            List<Long> batchIds,
            BigDecimal fallbackAdultPrice) {
        Map<String, Object> promotion = map(price.get("promotion"));
        if (promotion.isEmpty()) promotion = map(source.get("promotion"));
        BigDecimal adultPrice = effectiveFixedPrice(
                firstNonNull(price.get("adultPrice"), price.get("price")), fallbackAdultPrice);
        BigDecimal childPrice = positiveDecimal(price.get("childPrice"));
        BigDecimal originalAdultPrice = discountOriginal(price.get("originalAdultPrice"), adultPrice);
        BigDecimal originalChildPrice = discountOriginal(price.get("originalChildPrice"), childPrice);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("sourceId", text(source.get("id")));
        item.put("packageId", packageId);
        item.put("name", defaultText(source.get("name"), "套餐价"));
        item.put("adultPrice", adultPrice);
        item.put("childPrice", childPrice);
        item.put("originalAdultPrice", originalAdultPrice);
        item.put("originalChildPrice", originalChildPrice);
        item.put("adultDiscountLabel", discountLabel(promotion, originalAdultPrice, adultPrice));
        item.put("childDiscountLabel", discountLabel(promotion, originalChildPrice, childPrice));
        item.put("adultSavedAmount", savedAmount(originalAdultPrice, adultPrice));
        item.put("childSavedAmount", savedAmount(originalChildPrice, childPrice));
        item.put("batchIds", batchIds);
        item.put("status", 1);
        item.put("sortOrder", integer(source.get("sortOrder")));
        return item;
    }

    private List<Map<String, Object>> synthesizeSchedulePackagePrices(
            List<Map<String, Object>> remotePackages,
            List<Map<String, Object>> remoteSchedules,
            IdRegistry packageIds,
            IdRegistry scheduleIds,
            Map<Long, BigDecimal> packageBasePrices) {
        List<Map<String, Object>> result = new ArrayList<>();
        long nextId = 1;
        for (Map<String, Object> schedule : remoteSchedules) {
            List<String> applicable = stringList(schedule.get("packageIds"));
            if (applicable.isEmpty() && !text(schedule.get("packageId")).isBlank()) {
                applicable.add(text(schedule.get("packageId")));
            }
            for (Map<String, Object> pkg : remotePackages) {
                String remotePackageId = text(pkg.get("id"));
                if (!applicable.isEmpty() && !applicable.contains(remotePackageId)) {
                    continue;
                }
                Long packageId = packageIds.idOrNull(remotePackageId);
                Long batchId = scheduleIds.idOrNull(schedule.get("id"));
                if (packageId == null || batchId == null) {
                    continue;
                }
                Map<String, Object> price = new LinkedHashMap<>();
                price.put("adultPrice", firstNonNull(schedule.get("adultPrice"), pkg.get("price")));
                price.put("childPrice", schedule.get("childPrice"));
                price.put("originalAdultPrice", firstNonNull(schedule.get("originalAdultPrice"), pkg.get("originalPrice")));
                price.put("originalChildPrice", schedule.get("originalChildPrice"));
                price.put("promotion", schedule.get("promotion"));
                result.add(packagePriceItem(nextId++, pkg, price, packageId,
                        List.of(batchId), packageBasePrices.get(packageId)));
            }
        }
        return result;
    }

    private List<Map<String, Object>> adaptAddons(List<Map<String, Object>> remoteAddons, IdRegistry addonIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> source : remoteAddons) {
            if (!enabled(source.get("status"))) {
                continue;
            }
            BigDecimal fee = decimal(firstNonNull(source.get("extraFeePerPerson"), source.get("price")));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", addonIds.id(source.get("id")));
            item.put("sourceId", text(source.get("id")));
            item.put("name", defaultText(source.get("name"), "附加费用"));
            item.put("extraFeePerPerson", fee);
            item.put("description", source.get("description"));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> adaptAddonPriceItems(
            List<Map<String, Object>> remoteItems,
            IdRegistry addonIds,
            IdRegistry scheduleIds,
            IdRegistry packageIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        long nextId = 1;
        for (Map<String, Object> source : remoteItems) {
            Long addonId = addonIds.idOrNull(source.get("addonId"));
            if (addonId == null || !enabled(source.get("status"))) {
                continue;
            }
            Map<String, Object> schedulePrices = map(source.get("schedulePrices"));
            List<Long> mappedPackageIds = mapRelatedIds(source.get("packageIds"), packageIds);
            if (mappedPackageIds.isEmpty() && !text(source.get("packageId")).isBlank()) {
                Long packageId = packageIds.idOrNull(source.get("packageId"));
                if (packageId != null) mappedPackageIds.add(packageId);
            }
            if (!schedulePrices.isEmpty()) {
                for (Map.Entry<String, Object> entry : schedulePrices.entrySet()) {
                    Long batchId = scheduleIds.idOrNull(entry.getKey());
                    if (batchId != null) {
                        Map<String, Object> schedulePrice = map(entry.getValue());
                        List<Long> pricePackageIds = mapRelatedIds(schedulePrice.get("packageIds"), packageIds);
                        if (pricePackageIds.isEmpty()) pricePackageIds = mappedPackageIds;
                        result.add(addonPriceItem(nextId++, source, schedulePrice, addonId,
                                List.of(batchId), pricePackageIds));
                    }
                }
            } else {
                List<Long> batchIds = mapRelatedIds(source.get("scheduleIds"), scheduleIds);
                if (batchIds.isEmpty()) batchIds = scheduleIds.allIds();
                result.add(addonPriceItem(nextId++, source, source, addonId, batchIds, mappedPackageIds));
            }
        }
        return result;
    }

    private Map<String, Object> addonPriceItem(
            long id,
            Map<String, Object> source,
            Map<String, Object> price,
            Long addonId,
            List<Long> batchIds,
            List<Long> packageIds) {
        Map<String, Object> promotion = map(price.get("promotion"));
        if (promotion.isEmpty()) promotion = map(source.get("promotion"));
        BigDecimal salePrice = decimal(price.get("price"));
        BigDecimal originalPrice = discountOriginal(price.get("originalPrice"), salePrice);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("sourceId", text(source.get("id")));
        item.put("addonId", addonId);
        item.put("packageId", packageIds.size() == 1 ? packageIds.get(0) : null);
        item.put("packageIds", packageIds);
        item.put("name", defaultText(source.get("name"), "附加费用价"));
        item.put("price", salePrice);
        item.put("originalPrice", originalPrice);
        item.put("discountLabel", discountLabel(promotion, originalPrice, salePrice));
        item.put("savedAmount", savedAmount(originalPrice, salePrice));
        item.put("batchIds", batchIds);
        item.put("status", 1);
        item.put("sortOrder", integer(source.get("sortOrder")));
        return item;
    }

    private List<Map<String, Object>> filterTours(
            List<Map<String, Object>> source,
            RemoteQuery query,
            String ignoredGroup) {
        return source.stream().filter(item -> {
            if (!matchesKeyword(item, query.keyword())) return false;
            if (!matchesSearchIntent(item, query)) return false;
            if (!"tourType".equals(ignoredGroup) && !matchesValue(item.get("tourType"), query.tourType())) return false;
            if (!"city".equals(ignoredGroup) && !matchesPlace(item.get("city"), query.city())) return false;
            if (!"destination".equals(ignoredGroup) && !matchesPlace(item.get("destination"), query.destination())) return false;
            if (!"days".equals(ignoredGroup) && !matchesDays(integer(item.get("days")), query.days())) return false;
            if (!"month".equals(ignoredGroup) && !matchesValue(item.get("month"), query.month())) return false;
            return "priceRange".equals(ignoredGroup) || matchesPrice(item, query.priceRange());
        }).collect(Collectors.toList());
    }

    private boolean matchesKeyword(Map<String, Object> item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return matchesAnyToken(item, keyword);
    }

    private boolean matchesAnyToken(Map<String, Object> item, String keyword) {
        String haystack = String.join(" ", nonBlankValues(
                text(item.get("title")), text(item.get("subtitle")), text(item.get("destination")),
                text(item.get("city")), text(item.get("categoryLabel")),
                String.join(" ", stringList(item.get("tags"))), String.join(" ", stringList(item.get("themes")))
        )).toLowerCase(Locale.ROOT);
        for (String token : keyword.toLowerCase(Locale.ROOT).split("[\\s,，、]+")) {
            if (!token.isBlank() && haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSearchIntent(Map<String, Object> item, RemoteQuery query) {
        if ("cruise".equalsIgnoreCase(query.searchMode()) && !"cruise".equals(item.get("tourType"))) {
            return false;
        }
        if ("around".equalsIgnoreCase(query.searchMode()) && !"around".equals(item.get("tourType"))) {
            return false;
        }
        return query.intentDestination() == null || query.intentDestination().isBlank()
                || matchesPlace(item.get("destination"), query.intentDestination());
    }

    private boolean matchesValue(Object actual, String expected) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(text(actual));
    }

    private boolean matchesPlace(Object actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        String left = normalizePlace(text(actual));
        String right = normalizePlace(expected);
        return left.equals(right) || left.contains(right) || right.contains(left);
    }

    private String normalizePlace(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        Map<String, String> aliases = Map.ofEntries(
                Map.entry("xisha", "西沙"), Map.entry("sanxia", "三峡"), Map.entry("sanyan", "三峡"),
                Map.entry("chongqing", "重庆"), Map.entry("chengdu", "成都"), Map.entry("kunming", "昆明"),
                Map.entry("guiyang", "贵阳"), Map.entry("sanya", "三亚"), Map.entry("yichang", "宜昌"),
                Map.entry("beijing", "北京"), Map.entry("shanghai", "上海"), Map.entry("guangzhou", "广州"),
                Map.entry("shenzhen", "深圳"), Map.entry("hangzhou", "杭州"), Map.entry("xian", "西安")
        );
        text = aliases.getOrDefault(text, text);
        return text.replace("省", "").replace("市", "").replace("自治区", "").replace("特别行政区", "")
                .replace("/", "").replace(" ", "");
    }

    private boolean matchesDays(int actual, String expected) {
        if (expected == null || expected.isBlank()) return true;
        if (expected.matches("\\d+")) return actual == Integer.parseInt(expected);
        return switch (expected) {
            case "1-3" -> actual >= 1 && actual <= 3;
            case "4-6" -> actual >= 4 && actual <= 6;
            case "7-9" -> actual >= 7 && actual <= 9;
            case "10+" -> actual >= 10;
            default -> true;
        };
    }

    private boolean matchesPrice(Map<String, Object> item, String range) {
        if (range == null || range.isBlank()) return true;
        if ("inquiry".equalsIgnoreCase(text(item.get("pricingMode")))) return false;
        BigDecimal price = decimal(item.get("minPrice"));
        return switch (range) {
            case "0-500" -> price.compareTo(BigDecimal.valueOf(500)) <= 0;
            case "500-1000" -> price.compareTo(BigDecimal.valueOf(500)) > 0 && price.compareTo(BigDecimal.valueOf(1000)) <= 0;
            case "1000-2000" -> price.compareTo(BigDecimal.valueOf(1000)) > 0 && price.compareTo(BigDecimal.valueOf(2000)) <= 0;
            case "2000+" -> price.compareTo(BigDecimal.valueOf(2000)) > 0;
            default -> true;
        };
    }

    private void sortTours(List<Map<String, Object>> tours, String sortType) {
        Comparator<Map<String, Object>> defaultComparator = displayConfigService.defaultComparator();
        if ("price_asc".equals(sortType)) {
            tours.sort(Comparator
                    .comparing((Map<String, Object> item) -> "inquiry".equalsIgnoreCase(text(item.get("pricingMode"))))
                    .thenComparing(item -> decimal(item.get("minPrice")))
                    .thenComparing(defaultComparator));
        } else if ("price_desc".equals(sortType)) {
            tours.sort(Comparator
                    .comparing((Map<String, Object> item) -> "inquiry".equalsIgnoreCase(text(item.get("pricingMode"))))
                    .thenComparing((Map<String, Object> item) -> decimal(item.get("minPrice")), Comparator.reverseOrder())
                    .thenComparing(defaultComparator));
        } else if ("popular".equals(sortType)) {
            tours.sort(Comparator.comparingInt((Map<String, Object> item) -> integer(item.get("enrolledCount")))
                    .reversed().thenComparing(defaultComparator));
        } else {
            tours.sort(defaultComparator);
        }
    }

    private List<Map<String, Object>> countOptions(
            List<Map<String, Object>> tours,
            Function<Map<String, Object>, String> valueGetter) {
        Map<String, Long> counts = tours.stream()
                .map(valueGetter)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        counts.forEach((value, count) -> result.add(Map.of("value", value, "count", count)));
        return result;
    }

    private String resolveTourType(Map<String, Object> tour) {
        String content = String.join(" ", nonBlankValues(
                text(tour.get("categoryLabel")), String.join(" ", stringList(tour.get("tags"))),
                String.join(" ", stringList(tour.get("themes"))), text(tour.get("title"))
        ));
        if (content.contains("邮轮") || content.contains("游轮")) return "cruise";
        if (content.contains("亲子")) return "parent_child";
        if (content.contains("研学")) return "study";
        if (content.contains("徒步") || content.contains("户外")) return "outdoor";
        if (content.contains("摄影")) return "photography";
        if (content.contains("康养")) return "wellness";
        if (content.contains("自驾")) return "selfdrive";
        if (content.contains("自由行")) return "free";
        if (content.contains("定制")) return "custom";
        if (integer(tour.get("days")) <= 3) return "around";
        return "long";
    }

    private String resolveFeature(Map<String, Object> remote) {
        Set<String> features = new LinkedHashSet<>(stringList(remote.get("bookingFeatures")));
        List<Map<String, Object>> guarantees = mapList(remote.get("serviceGuarantees"));
        for (Map<String, Object> guarantee : guarantees) {
            String title = text(guarantee.get("title")).trim();
            if (!title.isBlank()) features.add(title);
        }
        if (features.isEmpty()) features.addAll(stringList(remote.get("highlights")));
        return features.isEmpty() ? text(remote.get("subtitle")) : String.join("，", features);
    }

    private List<String> displayTags(Map<String, Object> remote) {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(stringList(remote.get("tags")));
        result.addAll(stringList(remote.get("detailTags")));
        for (String theme : stringList(remote.get("themes"))) {
            result.add(switch (theme.toLowerCase(Locale.ROOT)) {
                case "city" -> "城市观光";
                case "ocean" -> "海岛度假";
                case "river" -> "游轮航线";
                case "train" -> "旅游专列";
                default -> theme;
            });
        }
        return new ArrayList<>(result);
    }

    private String buildDetailContent(Map<String, Object> remote) {
        StringBuilder html = new StringBuilder();
        appendRichOrTextSection(html, "邮轮简介", remote.get("highlightContent"),
                mergeStringLists(productIntroductions(remote), remote.get("highlights"),
                        remote.get("recommendedReason"), remote.get("bookingFeatures")));

        if (!text(remote.get("itineraryContent")).isBlank()) {
            appendRichSection(html, "行程详情", remote.get("itineraryContent"));
        } else if (!mapList(remote.get("itinerary")).isEmpty()) {
            appendItinerary(html, "行程详情", mapList(remote.get("itinerary")));
        } else {
            appendTextSection(html, "行程详情", List.of("暂无行程详情"));
        }

        List<String> feeDetails = new ArrayList<>();
        for (String item : stringList(remote.get("feeIncludes"))) feeDetails.add("费用包含：" + item);
        for (String item : stringList(remote.get("feeExcludes"))) feeDetails.add("费用不含：" + item);
        appendRichOrTextSection(html, "费用详情", remote.get("feeContent"), feeDetails);

        List<String> bookingNotices = mergeStringLists(
                remote.get("purchaseNotice"), remote.get("bookingNotice"), remote.get("itineraryNotice"));
        for (String item : stringList(remote.get("refundPolicy"))) bookingNotices.add("退订说明：" + item);
        appendRichOrTextSection(html, "预订须知", remote.get("noticeContent"), bookingNotices);
        return html.toString();
    }

    private void appendRichOrTextSection(
            StringBuilder html,
            String title,
            Object richContent,
            List<String> fallbackContent) {
        if (!text(richContent).isBlank()) {
            appendRichSection(html, title, richContent);
        } else {
            appendTextSection(html, title,
                    fallbackContent.isEmpty() ? List.of("暂无" + title) : fallbackContent);
        }
    }

    private List<String> productIntroductions(Map<String, Object> remote) {
        Set<String> result = new LinkedHashSet<>();
        Map<String, Object> cruiseBooking = map(remote.get("cruiseBooking"));
        for (Object value : new Object[]{
                remote.get("cruiseIntroduction"), cruiseBooking.get("introduction"),
                remote.get("subtitle"), remote.get("overview")}) {
            String introduction = text(value).trim();
            if (!introduction.isBlank()) result.add(introduction);
        }
        return new ArrayList<>(result);
    }

    private String featureText(List<String> introductions, List<String> features) {
        List<String> content = new ArrayList<>(new LinkedHashSet<>(introductions));
        List<String> remainingFeatures = features.stream()
                .filter(feature -> !content.contains(feature))
                .toList();
        if (!remainingFeatures.isEmpty()) content.add(String.join("；", remainingFeatures));
        return String.join("\n", content);
    }

    private Map<String, Object> adaptMedia(Map<String, Object> remote) {
        Set<String> images = new LinkedHashSet<>();
        String videoUrl = "";
        String videoPoster = "";
        for (Map<String, Object> media : mapList(remote.get("media"))) {
            String type = text(media.get("type")).trim().toLowerCase(Locale.ROOT);
            String url = text(media.get("url")).trim();
            String cover = text(media.get("cover")).trim();
            if ("image".equals(type) && !url.isBlank()) {
                images.add(url);
            } else if ("video".equals(type) && videoUrl.isBlank() && !url.isBlank()) {
                videoUrl = url;
                videoPoster = cover;
            } else if ("channel_video".equals(type) && !cover.isBlank()) {
                images.add(cover);
            }
        }
        images.addAll(stringList(remote.get("images")));
        String cover = text(remote.get("cover")).trim();
        if (!cover.isBlank()) images.add(cover);
        if (images.isEmpty() && !videoPoster.isBlank()) images.add(videoPoster);
        if (videoPoster.isBlank()) videoPoster = images.stream().findFirst().orElse("");

        Map<String, Object> video = new LinkedHashMap<>();
        video.put("url", videoUrl);
        video.put("poster", videoPoster);
        video.put("enabled", videoUrl.isBlank() ? 0 : 1);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("images", new ArrayList<>(images));
        result.put("video", video);
        return result;
    }

    private void appendRichSection(StringBuilder html, String title, Object content) {
        String value = text(content).trim();
        if (!value.isBlank()) {
            html.append("<section><h2>").append(title).append("</h2>").append(value).append("</section>");
        }
    }

    private void appendTextSection(StringBuilder html, String title, List<String> content) {
        List<String> values = content.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (values.isEmpty()) return;
        html.append("<section><h2>").append(title).append("</h2>");
        for (String value : values) {
            html.append("<p>").append(HtmlUtils.htmlEscape(value)).append("</p>");
        }
        html.append("</section>");
    }

    private void appendTravelProfile(StringBuilder html, Map<String, Object> remote) {
        List<String> items = new ArrayList<>();
        addLabeledItem(items, "成团人数", remote.get("groupSizeText"));
        addLabeledItem(items, "年龄范围", remote.get("ageRange"));
        List<String> suitableFor = stringList(remote.get("suitableFor"));
        if (!suitableFor.isEmpty()) items.add("适合人群：" + String.join("、", suitableFor));
        html.append(listSection("适用说明", items));
    }

    private void addLabeledItem(List<String> items, String label, Object value) {
        String content = text(value).trim();
        if (!content.isBlank()) items.add(label + "：" + content);
    }

    private void appendItinerary(StringBuilder html, String sectionTitle, List<Map<String, Object>> itinerary) {
        if (itinerary.isEmpty()) return;
        html.append("<section><h2>").append(sectionTitle).append("</h2>");
        for (Map<String, Object> day : itinerary) {
            html.append("<h3>第").append(integerOrDefault(day.get("day"), 1)).append("天 ")
                    .append(HtmlUtils.htmlEscape(text(day.get("title")))).append("</h3>");
            if (!text(day.get("routeTitle")).isBlank()) {
                html.append("<p><strong>").append(HtmlUtils.htmlEscape(text(day.get("routeTitle")))).append("</strong></p>");
            }
            List<String> profile = new ArrayList<>();
            addLabeledItem(profile, "城市", day.get("city"));
            addLabeledItem(profile, "交通", day.get("transport"));
            addLabeledItem(profile, "住宿", day.get("accommodation"));
            List<String> meals = stringList(day.get("meals"));
            if (!meals.isEmpty()) profile.add("餐食：" + String.join("、", meals));
            if (!profile.isEmpty()) {
                html.append("<p>").append(HtmlUtils.htmlEscape(String.join("；", profile))).append("</p>");
            }
            if (!text(day.get("description")).isBlank()) {
                html.append("<p>").append(HtmlUtils.htmlEscape(text(day.get("description")))).append("</p>");
            }
            List<String> attractions = stringList(day.get("attractions"));
            if (!attractions.isEmpty()) {
                html.append("<p><strong>游览：</strong>")
                        .append(HtmlUtils.htmlEscape(String.join("、", attractions))).append("</p>");
            }
            List<Map<String, Object>> nodes = mapList(day.get("nodes"));
            if (!nodes.isEmpty()) {
                html.append("<ul>");
                for (Map<String, Object> node : nodes) {
                    String nodeTitle = String.join(" ", nonBlankValues(
                            text(node.get("time")), text(node.get("title"))));
                    String nodeContent = text(node.get("content"));
                    html.append("<li>");
                    if (!nodeTitle.isBlank()) html.append("<strong>").append(HtmlUtils.htmlEscape(nodeTitle)).append("</strong>");
                    if (!nodeContent.isBlank()) html.append(" ").append(HtmlUtils.htmlEscape(nodeContent));
                    html.append("</li>");
                }
                html.append("</ul>");
            }
            for (String image : stringList(day.get("images"))) {
                html.append("<img src=\"").append(HtmlUtils.htmlEscape(image)).append("\" alt=\"行程图片\">");
            }
        }
        html.append("</section>");
    }

    private void appendGuarantees(StringBuilder html, List<Map<String, Object>> guarantees) {
        if (guarantees.isEmpty()) return;
        html.append("<section><h2>服务保障</h2><ul>");
        for (Map<String, Object> guarantee : guarantees) {
            String title = text(guarantee.get("title")).trim();
            String detail = firstNonBlank(guarantee.get("detail"), guarantee.get("description"));
            if (title.isBlank() && detail.isBlank()) continue;
            html.append("<li>");
            if (!title.isBlank()) html.append("<strong>").append(HtmlUtils.htmlEscape(title)).append("</strong>");
            if (!detail.isBlank()) html.append(title.isBlank() ? "" : "：").append(HtmlUtils.htmlEscape(detail));
            html.append("</li>");
        }
        html.append("</ul></section>");
    }

    private String listSection(String title, List<String> items) {
        if (items.isEmpty()) return "";
        StringBuilder html = new StringBuilder("<section><h2>").append(title).append("</h2><ul>");
        for (String item : items) {
            html.append("<li>").append(HtmlUtils.htmlEscape(item)).append("</li>");
        }
        return html.append("</ul></section>").toString();
    }

    private String joinNotices(Map<String, Object> remote) {
        List<String> notices = mergeStringLists(
                remote.get("purchaseNotice"), remote.get("bookingNotice"), remote.get("itineraryNotice"));
        return notices.isEmpty() ? "以当前商品展示规则为准" : String.join("；", notices);
    }

    private Map<String, Object> orderConfirmation(Map<String, Object> remote) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", bool(remote.get("orderConfirmationEnabled")));
        result.put("title", text(remote.get("orderConfirmationTitle")));
        result.put("subtitle", text(remote.get("orderConfirmationSubtitle")));
        result.put("content", text(remote.get("orderConfirmationContent")));
        result.put("contactText", text(remote.get("orderConfirmationContactText")));
        String confirmText = firstNonBlank(
                remote.get("orderConfirmationConfirmText"), remote.get("orderConfirmationButtonText"));
        result.put("confirmText", confirmText);
        result.put("buttonText", confirmText);
        return result;
    }

    private BigDecimal discountOriginal(Object originalValue, BigDecimal salePrice) {
        BigDecimal originalPrice = nullableDecimal(originalValue);
        return originalPrice != null && salePrice != null && originalPrice.compareTo(salePrice) > 0
                ? originalPrice : null;
    }

    private BigDecimal savedAmount(BigDecimal originalPrice, BigDecimal salePrice) {
        if (originalPrice == null || salePrice == null || originalPrice.compareTo(salePrice) <= 0) {
            return BigDecimal.ZERO;
        }
        return originalPrice.subtract(salePrice);
    }

    private String discountLabel(Map<String, Object> promotion, BigDecimal originalPrice, BigDecimal salePrice) {
        if (originalPrice == null || salePrice == null || originalPrice.compareTo(salePrice) <= 0) {
            return "";
        }
        return text(promotion.get("badgeText"));
    }

    private String encodeRemoteTourId(String remoteId) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(remoteId.getBytes(StandardCharsets.UTF_8));
        return TOUR_ID_PREFIX + encoded;
    }

    private String decodeTourId(String encodedId) {
        if (!isMiniappTourId(encodedId)) {
            throw new ServiceException("无效的小程序商品编号");
        }
        try {
            String value = encodedId.substring(TOUR_ID_PREFIX.length());
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            throw new ServiceException("无效的小程序商品编号");
        }
    }

    private void normalizeRemoteAssets(Object value, String apiBaseUrl) {
        String origin = apiOrigin(apiBaseUrl);
        normalizeRemoteAssets(value, origin, new java.util.IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Object normalizeRemoteAssets(Object value, String origin, java.util.IdentityHashMap<Object, Boolean> visited) {
        if (value == null) return null;
        if (value instanceof String text) return normalizeAssetText(text, origin);
        if (visited.put(value, Boolean.TRUE) != null) return value;
        if (value instanceof Map<?, ?> source) {
            ((Map<Object, Object>) source).replaceAll((key, child) -> normalizeRemoteAssets(child, origin, visited));
        } else if (value instanceof List<?> source) {
            List<Object> list = (List<Object>) source;
            for (int index = 0; index < list.size(); index++) {
                list.set(index, normalizeRemoteAssets(list.get(index), origin, visited));
            }
        }
        return value;
    }

    private String normalizeAssetText(String value, String origin) {
        if (!value.contains("uploads/")) return value;
        Matcher matcher = RELATIVE_UPLOAD.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(2).startsWith("/") ? matcher.group(2) : "/" + matcher.group(2);
            if (path.startsWith("/uploads/")) path = "/api" + path;
            matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(1) + origin + path));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String apiOrigin(String apiBaseUrl) {
        URI uri = URI.create(apiBaseUrl);
        int port = uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? "" : ":" + port);
    }

    private List<Long> mapRelatedIds(Object values, IdRegistry registry) {
        List<Long> result = new ArrayList<>();
        for (String value : stringList(values)) {
            Long mapped = registry.idOrNull(value);
            if (mapped != null && !result.contains(mapped)) result.add(mapped);
        }
        return result;
    }

    private String scheduleStatus(String status, int available) {
        if (available <= 0 || "full".equalsIgnoreCase(status) || "sold_out".equalsIgnoreCase(status)) return "已满";
        if ("closed".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)
                || "disabled".equalsIgnoreCase(status)) return "已截止";
        return "可报名";
    }

    private boolean enabled(Object status) {
        if (status == null) return true;
        if (status instanceof Number number) return number.intValue() == 1;
        String value = text(status).toLowerCase(Locale.ROOT);
        return Set.of("1", "true", "enabled", "available", "open").contains(value);
    }

    private String monthOf(String date) {
        try {
            return String.valueOf(LocalDate.parse(date).getMonthValue());
        } catch (Exception ignored) {
            return "";
        }
    }

    private String daysRange(int days) {
        if (days <= 0) return "";
        if (days <= 3) return "1-3";
        if (days <= 6) return "4-6";
        if (days <= 9) return "7-9";
        return "10+";
    }

    private String priceRange(BigDecimal price) {
        if (price.compareTo(BigDecimal.valueOf(500)) <= 0) return "0-500";
        if (price.compareTo(BigDecimal.valueOf(1000)) <= 0) return "500-1000";
        if (price.compareTo(BigDecimal.valueOf(2000)) <= 0) return "1000-2000";
        return "2000+";
    }

    private void addKeyword(Map<String, Integer> counts, String value) {
        if (value != null && !value.isBlank()) counts.merge(value, 1, Integer::sum);
    }

    private List<String> nonBlankValues(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) if (value != null && !value.isBlank()) result.add(value);
        return result;
    }

    private List<String> mergeStringLists(Object... values) {
        Set<String> result = new LinkedHashSet<>();
        for (Object value : values) result.addAll(stringList(value));
        return new ArrayList<>(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof Collection<?> collection)) return new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Map<?, ?>) result.add(map(item));
        }
        return result;
    }

    private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                String text = text(item).trim();
                if (!text.isBlank()) result.add(text);
            }
        } else if (value != null && !text(value).isBlank()) {
            String source = text(value).trim();
            if (source.startsWith("[") && source.endsWith("]")) {
                source = source.substring(1, source.length() - 1).replace("\"", "");
            }
            for (String item : source.split("[,，、]")) {
                if (!item.trim().isBlank()) result.add(item.trim());
            }
        }
        return result;
    }

    private String first(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) if (value != null) return value;
        return null;
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) if (!text(value).isBlank()) return text(value);
        return "";
    }

    private String defaultText(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int integerOrDefault(Object value, int fallback) {
        int parsed = integer(value);
        return parsed == 0 ? fallback : parsed;
    }

    private int firstInteger(Object... values) {
        for (Object value : values) {
            if (value != null) return integer(value);
        }
        return 0;
    }

    private long longValue(Object value, long fallback) {
        if (value instanceof Number number) return number.longValue();
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try {
            return value == null || text(value).isBlank() ? BigDecimal.ZERO : new BigDecimal(text(value));
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal decimalOrDefault(Object value, BigDecimal fallback) {
        return value == null ? fallback : decimal(value);
    }

    private BigDecimal effectiveFixedPrice(Object value, Object fallbackValue) {
        BigDecimal price = nullableDecimal(value);
        BigDecimal fallback = nullableDecimal(fallbackValue);
        if (price == null || (price.compareTo(BigDecimal.ZERO) <= 0
                && fallback != null && fallback.compareTo(BigDecimal.ZERO) > 0)) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        return price;
    }

    private BigDecimal nullableDecimal(Object value) {
        return value == null || text(value).isBlank() ? null : decimal(value);
    }

    private BigDecimal positiveDecimal(Object value) {
        BigDecimal decimal = nullableDecimal(value);
        return decimal != null && decimal.compareTo(BigDecimal.ZERO) > 0 ? decimal : null;
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean bool) return bool;
        return "true".equalsIgnoreCase(text(value)) || "1".equals(text(value));
    }

    private record RemoteQuery(
            String keyword,
            String tourType,
            String city,
            String destination,
            String days,
            String month,
            String priceRange,
            String searchMode,
            String intentDestination,
            String matchMode) {
    }

    private record CruiseProductAdaptation(
            List<Map<String, Object>> packages,
            List<Map<String, Object>> schedules,
            List<Map<String, Object>> packagePrices) {
    }

    private static final class IdRegistry {
        private final Map<String, Long> values = new LinkedHashMap<>();

        private IdRegistry(List<Map<String, Object>> items) {
            long next = 1;
            for (Map<String, Object> item : items) {
                String value = item.get("id") == null ? "" : String.valueOf(item.get("id"));
                if (!value.isBlank() && !values.containsKey(value)) values.put(value, next++);
            }
        }

        private Long id(Object value) {
            String key = value == null ? "" : String.valueOf(value);
            Long result = values.get(key);
            if (result == null) {
                result = (long) values.size() + 1;
                values.put(key, result);
            }
            return result;
        }

        private Long idOrNull(Object value) {
            return value == null ? null : values.get(String.valueOf(value));
        }

        private List<Long> allIds() {
            return new ArrayList<>(values.values());
        }
    }
}
