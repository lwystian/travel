package org.example.springboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.dto.TourOrderCreateDTO;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MiniappTourOrderBridgeService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private MiniappTourAdapterService adapterService;

    @Resource
    private MiniappInventoryService inventoryService;

    @Resource
    private TourProductSourceConfigService sourceConfigService;

    public boolean supports(TourOrderCreateDTO dto) {
        return dto != null && MiniappTourAdapterService.SOURCE_TYPE.equalsIgnoreCase(text(dto.getSourceType()));
    }

    public PreparedMiniappOrder prepare(TourOrderCreateDTO dto) {
        if (!sourceConfigService.isMiniappMode()) {
            throw new ServiceException("官网商品来源已切换，请刷新页面后重新选择");
        }
        String sourceTourId = required(dto.getSourceTourId(), "小程序商品编号缺失，请刷新后重试");
        Map<String, Object> detail = adapterService.getTourDetail(adapterService.encodeTourId(sourceTourId));
        Map<String, Object> tour = map(detail.get("tour"));

        Map<String, Object> selectedPackage = select(
                mapList(detail.get("tripPackages")), dto.getSourcePackageId(), dto.getTripPackageId(), "套餐");
        String sourcePackageId = required(text(selectedPackage.get("sourceId")), "小程序套餐编号缺失，请刷新后重试");
        Map<String, Object> selectedSchedule = select(
                mapList(detail.get("batchDates")), dto.getSourceScheduleId(), null, "出发班期");
        String sourceScheduleId = required(text(selectedSchedule.get("sourceId")), "小程序班期编号缺失，请刷新后重试");
        if (!text(selectedSchedule.get("date")).equals(text(dto.getBatchDate()))) {
            throw new ServiceException("出发日期已变更，请刷新页面后重试");
        }
        if (!containsId(selectedSchedule.get("packageIds"), selectedPackage.get("id"))) {
            throw new ServiceException("该班期不支持所选套餐，请重新选择");
        }
        if (!"可报名".equals(text(selectedSchedule.get("status")))) {
            throw new ServiceException("该班期" + defaultText(selectedSchedule.get("status"), "暂不可预订"));
        }

        List<Map<String, Object>> packagePriceItems = mapList(detail.get("packagePriceItems"));
        Map<String, Object> packagePriceItem = selectPriceItem(
                packagePriceItems, dto.getSourcePackagePriceItemId(),
                selectedPackage.get("id"), selectedSchedule.get("id"));
        boolean packageHasConfiguredPrices = packagePriceItems.stream()
                .anyMatch(item -> integer(item.get("status")) == 1
                        && sameNumber(item.get("packageId"), selectedPackage.get("id")));
        if (packagePriceItem == null && packageHasConfiguredPrices) {
            throw new ServiceException("该班期不支持所选套餐价格，请重新选择");
        }
        BigDecimal adultPrice = packagePriceItem == null
                ? decimal(selectedPackage.get("adultPrice")) : decimal(packagePriceItem.get("adultPrice"));
        BigDecimal childPrice = packagePriceItem == null
                ? nullableDecimal(selectedPackage.get("childPrice")) : nullableDecimal(packagePriceItem.get("childPrice"));
        int childCount = dto.getChildCount() == null ? 0 : dto.getChildCount();
        if (childCount > 0 && childPrice == null) {
            throw new ServiceException("所选套餐未设置儿童价格，请将儿童数量调整为0");
        }

        AddonResult addonResult = resolveAddons(dto, detail, selectedPackage, selectedSchedule, sourceTourId);
        TourBatch batch = inventoryService.syncBatch(sourceTourId, selectedSchedule);
        long tourId = inventoryService.stableNegativeId("tour", sourceTourId, sourceTourId);
        long packageId = inventoryService.stableNegativeId("package", sourceTourId, sourcePackageId);
        String sourcePriceItemId = packagePriceItem == null ? "" : text(packagePriceItem.get("sourceId"));
        Long packagePriceItemId = packagePriceItem == null ? null
                : inventoryService.stableNegativeId("package-price", sourceTourId,
                sourcePriceItemId + "|" + sourceScheduleId + "|" + sourcePackageId);

        return new PreparedMiniappOrder(
                tourId,
                packageId,
                packagePriceItemId,
                batch,
                sourceTourId,
                sourcePackageId,
                sourceScheduleId,
                sourcePriceItemId,
                defaultText(tour.get("title"), "小程序行程"),
                defaultText(tour.get("code"), "MINIAPP"),
                defaultText(selectedPackage.get("name"), "标准套餐"),
                adultPrice,
                childPrice == null ? BigDecimal.ZERO : childPrice,
                addonResult.totalAmount(),
                addonResult.primaryAddonId(),
                addonResult.summary(),
                addonResult.itemsJson()
        );
    }

    private AddonResult resolveAddons(TourOrderCreateDTO dto,
                                      Map<String, Object> detail,
                                      Map<String, Object> selectedPackage,
                                      Map<String, Object> selectedSchedule,
                                      String sourceTourId) {
        if (dto.getAddonSelections() == null || dto.getAddonSelections().isEmpty()) {
            return AddonResult.EMPTY;
        }
        List<Map<String, Object>> addons = mapList(detail.get("batchPackages"));
        List<Map<String, Object>> prices = mapList(detail.get("addonPriceItems"));
        List<Map<String, Object>> snapshots = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Long primaryId = null;

        for (TourOrderCreateDTO.AddonSelection selection : dto.getAddonSelections()) {
            if (selection == null || selection.getQuantity() == null || selection.getQuantity() <= 0) continue;
            Map<String, Object> addon = select(addons, selection.getSourceAddonId(), selection.getBatchPackageId(), "附加费用");
            if (!containsId(selectedSchedule.get("addonIds"), addon.get("id"))) {
                throw new ServiceException("当前班期不支持所选附加费用");
            }
            Map<String, Object> priceItem = selectAddonPriceItem(
                    prices, selection.getSourceAddonPriceItemId(), addon.get("id"),
                    selectedPackage.get("id"), selectedSchedule.get("id"));
            boolean addonHasConfiguredPrices = prices.stream()
                    .anyMatch(item -> integer(item.get("status")) == 1
                            && sameNumber(item.get("addonId"), addon.get("id")));
            if (priceItem == null && addonHasConfiguredPrices) {
                throw new ServiceException("当前班期和套餐不支持所选附加费用");
            }
            BigDecimal unitPrice = priceItem == null
                    ? decimal(addon.get("extraFeePerPerson")) : decimal(priceItem.get("price"));
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(selection.getQuantity()));
            total = total.add(amount);
            String sourceAddonId = required(text(addon.get("sourceId")), "附加费用编号缺失，请刷新后重试");
            long addonId = inventoryService.stableNegativeId("addon", sourceTourId, sourceAddonId);
            if (primaryId == null) primaryId = addonId;

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", addonId);
            snapshot.put("sourceAddonId", sourceAddonId);
            snapshot.put("sourcePriceItemId", priceItem == null ? "" : text(priceItem.get("sourceId")));
            snapshot.put("name", defaultText(addon.get("name"), "附加费用"));
            snapshot.put("unitPrice", unitPrice);
            snapshot.put("quantity", selection.getQuantity());
            snapshot.put("amount", amount);
            snapshots.add(snapshot);
            summaries.add(snapshot.get("name") + " x" + selection.getQuantity());
        }
        if (snapshots.isEmpty()) return AddonResult.EMPTY;
        try {
            return new AddonResult(total, primaryId, String.join("，", summaries),
                    OBJECT_MAPPER.writeValueAsString(snapshots));
        } catch (Exception ex) {
            throw new ServiceException("附加费用快照生成失败，请稍后重试");
        }
    }

    private Map<String, Object> selectPriceItem(List<Map<String, Object>> items, String sourceId,
                                                Object packageId, Object batchId) {
        return items.stream()
                .filter(item -> integer(item.get("status")) == 1)
                .filter(item -> sameNumber(item.get("packageId"), packageId))
                .filter(item -> containsId(item.get("batchIds"), batchId))
                .filter(item -> text(sourceId).isBlank() || text(sourceId).equals(text(item.get("sourceId"))))
                .findFirst().orElse(null);
    }

    private Map<String, Object> selectAddonPriceItem(List<Map<String, Object>> items, String sourceId,
                                                     Object addonId, Object packageId, Object batchId) {
        return items.stream()
                .filter(item -> integer(item.get("status")) == 1)
                .filter(item -> sameNumber(item.get("addonId"), addonId))
                .filter(item -> containsId(item.get("batchIds"), batchId))
                .filter(item -> list(item.get("packageIds")).isEmpty() || containsId(item.get("packageIds"), packageId))
                .filter(item -> text(sourceId).isBlank() || text(sourceId).equals(text(item.get("sourceId"))))
                .findFirst().orElse(null);
    }

    private Map<String, Object> select(List<Map<String, Object>> items, String sourceId, Long adaptedId, String label) {
        String externalId = text(sourceId);
        Map<String, Object> selected = items.stream()
                .filter(item -> !externalId.isBlank() && externalId.equals(text(item.get("sourceId"))))
                .findFirst().orElse(null);
        if (selected == null && adaptedId != null) {
            selected = items.stream().filter(item -> sameNumber(item.get("id"), adaptedId)).findFirst().orElse(null);
        }
        if (selected == null) throw new ServiceException(label + "不存在或已下架，请刷新页面后重试");
        return selected;
    }

    private boolean containsId(Object values, Object expected) {
        List<?> ids = list(values);
        return ids.isEmpty() || ids.stream().anyMatch(value -> sameNumber(value, expected));
    }

    private boolean sameNumber(Object left, Object right) {
        if (left == null || right == null) return false;
        try {
            return Long.parseLong(text(left)) == Long.parseLong(text(right));
        } catch (Exception ignored) {
            return text(left).equals(text(right));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> source ? (Map<String, Object>) source : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> source)) return List.of();
        return source.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList();
    }

    private List<?> list(Object value) {
        return value instanceof List<?> source ? source : List.of();
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        try {
            return new BigDecimal(text(value));
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal nullableDecimal(Object value) {
        return value == null || text(value).isBlank() ? null : decimal(value);
    }

    private int integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(text(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new ServiceException(message);
        return value.trim();
    }

    private String defaultText(Object value, String fallback) {
        String result = text(value);
        return result.isBlank() ? fallback : result;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public record PreparedMiniappOrder(
            Long tourId,
            Long packageId,
            Long packagePriceItemId,
            TourBatch batch,
            String sourceTourId,
            String sourcePackageId,
            String sourceScheduleId,
            String sourcePackagePriceItemId,
            String tourName,
            String tourCode,
            String packageName,
            BigDecimal adultPrice,
            BigDecimal childPrice,
            BigDecimal addonAmount,
            Long primaryAddonId,
            String addonSummary,
            String addonItemsJson) {
    }

    private record AddonResult(BigDecimal totalAmount, Long primaryAddonId, String summary, String itemsJson) {
        private static final AddonResult EMPTY = new AddonResult(BigDecimal.ZERO, null, "无", null);
    }
}
