package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.BatchPackage;
import org.example.springboot.entity.TourAddonPriceItem;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.entity.TourPackage;
import org.example.springboot.entity.TourPackagePriceItem;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.BatchPackageMapper;
import org.example.springboot.mapper.TourAddonPriceItemMapper;
import org.example.springboot.mapper.TourBatchMapper;
import org.example.springboot.mapper.TourPackageMapper;
import org.example.springboot.mapper.TourPackagePriceItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TourPriceItemService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private TourPackagePriceItemMapper packagePriceItemMapper;

    @Resource
    private TourAddonPriceItemMapper addonPriceItemMapper;

    @Resource
    private TourPackageMapper tourPackageMapper;

    @Resource
    private BatchPackageMapper batchPackageMapper;

    @Resource
    private TourBatchMapper tourBatchMapper;

    public List<TourPackagePriceItem> getPackagePriceItemsByTourId(Long tourId) {
        if (tourId == null) {
            return List.of();
        }
        return packagePriceItemMapper.selectList(new LambdaQueryWrapper<TourPackagePriceItem>()
                .eq(TourPackagePriceItem::getTourId, tourId)
                .orderByAsc(TourPackagePriceItem::getPackageId)
                .orderByAsc(TourPackagePriceItem::getSortOrder)
                .orderByAsc(TourPackagePriceItem::getId));
    }

    public List<TourPackagePriceItem> getPackagePriceItems(Long packageId) {
        if (packageId == null) {
            return List.of();
        }
        return packagePriceItemMapper.selectList(new LambdaQueryWrapper<TourPackagePriceItem>()
                .eq(TourPackagePriceItem::getPackageId, packageId)
                .orderByAsc(TourPackagePriceItem::getSortOrder)
                .orderByAsc(TourPackagePriceItem::getId));
    }

    public List<TourAddonPriceItem> getAddonPriceItemsByTourId(Long tourId) {
        if (tourId == null) {
            return List.of();
        }
        return addonPriceItemMapper.selectList(new LambdaQueryWrapper<TourAddonPriceItem>()
                .eq(TourAddonPriceItem::getTourId, tourId)
                .orderByAsc(TourAddonPriceItem::getAddonId)
                .orderByAsc(TourAddonPriceItem::getPackageId)
                .orderByAsc(TourAddonPriceItem::getSortOrder)
                .orderByAsc(TourAddonPriceItem::getId));
    }

    public List<TourAddonPriceItem> getAddonPriceItems(Long addonId) {
        if (addonId == null) {
            return List.of();
        }
        return addonPriceItemMapper.selectList(new LambdaQueryWrapper<TourAddonPriceItem>()
                .eq(TourAddonPriceItem::getAddonId, addonId)
                .orderByAsc(TourAddonPriceItem::getPackageId)
                .orderByAsc(TourAddonPriceItem::getSortOrder)
                .orderByAsc(TourAddonPriceItem::getId));
    }

    public boolean hasActivePackagePriceItems(Long tourId) {
        if (tourId == null) {
            return false;
        }
        Long count = packagePriceItemMapper.selectCount(new LambdaQueryWrapper<TourPackagePriceItem>()
                .eq(TourPackagePriceItem::getTourId, tourId)
                .eq(TourPackagePriceItem::getStatus, 1));
        return count != null && count > 0;
    }

    public boolean hasActiveAddonPriceItems(Long addonId) {
        if (addonId == null) {
            return false;
        }
        Long count = addonPriceItemMapper.selectCount(new LambdaQueryWrapper<TourAddonPriceItem>()
                .eq(TourAddonPriceItem::getAddonId, addonId)
                .eq(TourAddonPriceItem::getStatus, 1));
        return count != null && count > 0;
    }

    public TourPackagePriceItem resolvePackagePriceItem(Long tourId, Long packageId, Long batchId, Long requestedPriceItemId, boolean requireWhenConfigured) {
        if (tourId == null || packageId == null || batchId == null) {
            return null;
        }
        if (requestedPriceItemId != null) {
            TourPackagePriceItem item = packagePriceItemMapper.selectById(requestedPriceItemId);
            if (!isPackagePriceItemMatched(item, tourId, packageId, batchId)) {
                throw new ServiceException("套餐价格项与当前出发日期不匹配");
            }
            return item;
        }
        List<TourPackagePriceItem> matchedItems = getPackagePriceItems(packageId).stream()
                .filter(item -> isPackagePriceItemMatched(item, tourId, packageId, batchId))
                .toList();
        if (matchedItems.size() > 1) {
            throw new ServiceException("该套餐在当前出发日期存在多个启用价格项，请联系管理员处理");
        }
        if (matchedItems.isEmpty() && requireWhenConfigured && hasActivePackagePriceItems(tourId)) {
            throw new ServiceException("该出发日期暂无所选套餐价格，请重新选择");
        }
        return matchedItems.isEmpty() ? null : matchedItems.get(0);
    }

    public TourAddonPriceItem resolveAddonPriceItem(Long tourId, Long addonId, Long batchId, Long packageId,
                                                    Long requestedPriceItemId) {
        if (tourId == null || addonId == null || batchId == null) {
            return null;
        }
        TourAddonPriceItem requestedItem = null;
        if (requestedPriceItemId != null) {
            requestedItem = addonPriceItemMapper.selectById(requestedPriceItemId);
            if (!isAddonPriceItemMatched(requestedItem, tourId, addonId, batchId, packageId)) {
                throw new ServiceException("附加费用价格项与当前出发日期或行程套餐不匹配");
            }
        }
        List<TourAddonPriceItem> matchedItems = getAddonPriceItems(addonId).stream()
                .filter(item -> isAddonPriceItemMatched(item, tourId, addonId, batchId, packageId))
                .toList();
        List<TourAddonPriceItem> packageItems = packageId == null ? List.of() : matchedItems.stream()
                .filter(item -> parseAddonPackageIds(item).contains(packageId))
                .toList();
        if (packageItems.size() > 1) {
            throw new ServiceException("该附加费用在当前出发日期和套餐存在多个启用价格项，请联系管理员处理");
        }
        TourAddonPriceItem resolvedItem;
        if (!packageItems.isEmpty()) {
            resolvedItem = packageItems.get(0);
        } else {
            List<TourAddonPriceItem> genericItems = matchedItems.stream()
                    .filter(item -> parseAddonPackageIds(item).isEmpty())
                    .toList();
            if (genericItems.size() > 1) {
                throw new ServiceException("该附加费用在当前出发日期存在多个通用价格项，请联系管理员处理");
            }
            resolvedItem = genericItems.isEmpty() ? null : genericItems.get(0);
        }
        if (requestedItem != null && (resolvedItem == null || !Objects.equals(requestedItem.getId(), resolvedItem.getId()))) {
            throw new ServiceException("附加费用价格已变更，请刷新页面后重试");
        }
        return resolvedItem;
    }

    @Transactional
    public TourPackagePriceItem savePackagePriceItem(Long packageId, TourPackagePriceItem item) {
        TourPackage tourPackage = tourPackageMapper.selectById(packageId);
        if (tourPackage == null) {
            throw new ServiceException("套餐不存在");
        }
        TourPackagePriceItem normalized = normalizePackagePriceItem(tourPackage, item);
        assertNoPackageBatchConflict(normalized);
        if (normalized.getId() == null) {
            normalized.setCreateTime(LocalDateTime.now());
            normalized.setUpdateTime(LocalDateTime.now());
            packagePriceItemMapper.insert(normalized);
        } else {
            normalized.setUpdateTime(LocalDateTime.now());
            packagePriceItemMapper.updateById(normalized);
        }
        return packagePriceItemMapper.selectById(normalized.getId());
    }

    @Transactional
    public TourAddonPriceItem saveAddonPriceItem(Long addonId, TourAddonPriceItem item) {
        BatchPackage addon = batchPackageMapper.selectById(addonId);
        if (addon == null) {
            throw new ServiceException("附加费用不存在");
        }
        TourAddonPriceItem normalized = normalizeAddonPriceItem(addon, item);
        assertNoAddonBatchConflict(normalized);
        if (normalized.getId() == null) {
            normalized.setCreateTime(LocalDateTime.now());
            normalized.setUpdateTime(LocalDateTime.now());
            addonPriceItemMapper.insert(normalized);
        } else {
            normalized.setUpdateTime(LocalDateTime.now());
            addonPriceItemMapper.updateById(normalized);
        }
        return addonPriceItemMapper.selectById(normalized.getId());
    }

    @Transactional
    public void deletePackagePriceItem(Long packageId, Long itemId) {
        TourPackagePriceItem item = packagePriceItemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getPackageId(), packageId)) {
            throw new ServiceException("套餐价格项不存在");
        }
        packagePriceItemMapper.deleteById(itemId);
    }

    @Transactional
    public void deleteAddonPriceItem(Long addonId, Long itemId) {
        TourAddonPriceItem item = addonPriceItemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getAddonId(), addonId)) {
            throw new ServiceException("附加费用价格项不存在");
        }
        addonPriceItemMapper.deleteById(itemId);
    }

    @Transactional
    public void deletePriceItemsByBatch(Long batchId) {
        if (batchId == null) {
            return;
        }
        removeBatchIdFromPackageItems(batchId);
        removeBatchIdFromAddonItems(batchId);
    }

    public Set<Long> parseBatchIds(String value) {
        if (!StringUtils.isNotBlank(value)) {
            return new LinkedHashSet<>();
        }
        String text = value.trim();
        try {
            if (text.startsWith("[")) {
                List<Long> parsed = OBJECT_MAPPER.readValue(text, new TypeReference<List<Long>>() {});
                return parsed.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
            }
        } catch (Exception ignored) {
        }
        return java.util.Arrays.stream(text.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(part -> {
                    try {
                        return Long.parseLong(part);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Long> parseAddonPackageIds(TourAddonPriceItem item) {
        if (item == null) {
            return new LinkedHashSet<>();
        }
        Set<Long> packageIds = parseBatchIds(item.getPackageIds());
        if (packageIds.isEmpty() && item.getPackageId() != null) {
            packageIds.add(item.getPackageId());
        }
        return packageIds;
    }

    public String serializeBatchIds(Set<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return "";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(new ArrayList<>(batchIds));
        } catch (Exception e) {
            throw new ServiceException("班期数据保存失败");
        }
    }

    private TourPackagePriceItem normalizePackagePriceItem(TourPackage tourPackage, TourPackagePriceItem source) {
        if (source == null) {
            throw new ServiceException("价格项不能为空");
        }
        TourPackagePriceItem item = new TourPackagePriceItem();
        if (source.getId() != null) {
            TourPackagePriceItem existing = packagePriceItemMapper.selectById(source.getId());
            if (existing == null || !Objects.equals(existing.getPackageId(), tourPackage.getId())) {
                throw new ServiceException("套餐价格项不存在");
            }
            item.setId(existing.getId());
            item.setCreateTime(existing.getCreateTime());
        }
        item.setTourId(tourPackage.getTourId());
        item.setPackageId(tourPackage.getId());
        item.setName(StringUtils.defaultIfBlank(source.getName(), "价格项"));
        BigDecimal adultPrice = positiveAmount(source.getAdultPrice(), "成人售价必须大于0");
        item.setAdultPrice(adultPrice);
        BigDecimal childPrice = optionalPositiveAmount(source.getChildPrice(), "儿童售价不能小于0");
        item.setChildPrice(childPrice);
        item.setOriginalAdultPrice(validateOriginal(source.getOriginalAdultPrice(), adultPrice, "成人划线价必须高于成人售价"));
        if (source.getOriginalChildPrice() != null && childPrice == null) {
            throw new ServiceException("填写儿童划线价时必须先设置儿童售价");
        }
        item.setOriginalChildPrice(validateOriginal(source.getOriginalChildPrice(), childPrice, "儿童划线价必须高于儿童售价"));
        item.setBatchIds(serializeBatchIds(requireTourBatchIds(tourPackage.getTourId(), parseBatchIds(source.getBatchIds()))));
        item.setStatus(source.getStatus() == null ? 1 : source.getStatus());
        item.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
        return item;
    }

    private TourAddonPriceItem normalizeAddonPriceItem(BatchPackage addon, TourAddonPriceItem source) {
        if (source == null) {
            throw new ServiceException("价格项不能为空");
        }
        TourAddonPriceItem item = new TourAddonPriceItem();
        if (source.getId() != null) {
            TourAddonPriceItem existing = addonPriceItemMapper.selectById(source.getId());
            if (existing == null || !Objects.equals(existing.getAddonId(), addon.getId())) {
                throw new ServiceException("附加费用价格项不存在");
            }
            item.setId(existing.getId());
            item.setCreateTime(existing.getCreateTime());
        }
        item.setTourId(addon.getTourId());
        item.setAddonId(addon.getId());
        Set<Long> packageIds = requireTourPackageIds(addon.getTourId(), parseAddonPackageIds(source));
        Set<Long> batchIds = requireTourBatchIds(addon.getTourId(), parseBatchIds(source.getBatchIds()));
        Integer status = source.getStatus() == null ? 1 : source.getStatus();
        if (Integer.valueOf(1).equals(status)) {
            requirePackagesAvailableForBatches(addon.getTourId(), packageIds, batchIds);
        }
        item.setPackageIds(serializeBatchIds(packageIds));
        item.setPackageId(packageIds.size() == 1 ? packageIds.iterator().next() : null);
        item.setName(StringUtils.defaultIfBlank(source.getName(), "价格项"));
        BigDecimal price = positiveAmount(source.getPrice(), "附加费用售价必须大于0");
        item.setPrice(price);
        item.setOriginalPrice(null);
        item.setBatchIds(serializeBatchIds(batchIds));
        item.setStatus(status);
        item.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
        return item;
    }

    private Set<Long> requireTourBatchIds(Long tourId, Set<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<TourBatch> batches = tourBatchMapper.selectList(new LambdaQueryWrapper<TourBatch>()
                .eq(TourBatch::getTourId, tourId)
                .in(TourBatch::getId, batchIds));
        Set<Long> validIds = batches.stream().map(TourBatch::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (validIds.size() != batchIds.size()) {
            throw new ServiceException("存在不属于当前行程的出发班期");
        }
        return validIds;
    }

    private Set<Long> requireTourPackageIds(Long tourId, Set<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<TourPackage> packages = tourPackageMapper.selectList(new LambdaQueryWrapper<TourPackage>()
                .eq(TourPackage::getTourId, tourId)
                .in(TourPackage::getId, packageIds));
        Set<Long> validIds = packages.stream()
                .map(TourPackage::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (validIds.size() != packageIds.size()) {
            throw new ServiceException("存在不属于当前行程的适用套餐");
        }
        return validIds;
    }

    private void requirePackagesAvailableForBatches(Long tourId, Set<Long> packageIds, Set<Long> batchIds) {
        if (packageIds == null || packageIds.isEmpty() || batchIds == null || batchIds.isEmpty()) {
            return;
        }
        List<TourBatch> batches = tourBatchMapper.selectList(new LambdaQueryWrapper<TourBatch>()
                .eq(TourBatch::getTourId, tourId)
                .in(TourBatch::getId, batchIds));
        Map<Long, TourBatch> batchMap = batches.stream()
                .collect(Collectors.toMap(TourBatch::getId, batch -> batch, (left, right) -> left));

        if (hasActivePackagePriceItems(tourId)) {
            Map<Long, Set<Long>> availableBatchMap = packageBatchMap(tourId);
            for (Long packageId : packageIds) {
                Set<Long> availableBatchIds = availableBatchMap.getOrDefault(packageId, Set.of());
                for (Long batchId : batchIds) {
                    if (!availableBatchIds.contains(batchId)) {
                        throw unavailablePackageBatchException(packageId, batchMap.get(batchId));
                    }
                }
            }
            return;
        }

        for (Long batchId : batchIds) {
            TourBatch batch = batchMap.get(batchId);
            Set<Long> availablePackageIds = batch == null ? Set.of() : parseBatchIds(batch.getPackageIds());
            if (availablePackageIds.isEmpty()) {
                continue;
            }
            for (Long packageId : packageIds) {
                if (!availablePackageIds.contains(packageId)) {
                    throw unavailablePackageBatchException(packageId, batch);
                }
            }
        }
    }

    private ServiceException unavailablePackageBatchException(Long packageId, TourBatch batch) {
        String batchLabel = batch != null && batch.getDepartureDate() != null
                ? batch.getDepartureDate().toString()
                : String.valueOf(batch == null ? "" : batch.getId());
        return new ServiceException("套餐ID " + packageId + " 不适用于出发日期 " + batchLabel + "，请调整适用套餐或日期");
    }

    private void assertNoPackageBatchConflict(TourPackagePriceItem target) {
        if (!Integer.valueOf(1).equals(target.getStatus())) {
            return;
        }
        Set<Long> targetBatchIds = parseBatchIds(target.getBatchIds());
        if (targetBatchIds.isEmpty()) {
            return;
        }
        List<TourPackagePriceItem> siblings = packagePriceItemMapper.selectList(new LambdaQueryWrapper<TourPackagePriceItem>()
                .eq(TourPackagePriceItem::getPackageId, target.getPackageId())
                .eq(TourPackagePriceItem::getStatus, 1));
        for (TourPackagePriceItem sibling : siblings) {
            if (Objects.equals(sibling.getId(), target.getId())) {
                continue;
            }
            Set<Long> duplicate = parseBatchIds(sibling.getBatchIds());
            duplicate.retainAll(targetBatchIds);
            if (!duplicate.isEmpty()) {
                throw new ServiceException("同一套餐的同一出发日期只能绑定一个启用价格项");
            }
        }
    }

    private void assertNoAddonBatchConflict(TourAddonPriceItem target) {
        if (!Integer.valueOf(1).equals(target.getStatus())) {
            return;
        }
        Set<Long> targetBatchIds = parseBatchIds(target.getBatchIds());
        if (targetBatchIds.isEmpty()) {
            return;
        }
        List<TourAddonPriceItem> siblings = addonPriceItemMapper.selectList(new LambdaQueryWrapper<TourAddonPriceItem>()
                .eq(TourAddonPriceItem::getAddonId, target.getAddonId())
                .eq(TourAddonPriceItem::getStatus, 1));
        Set<Long> targetPackageIds = parseAddonPackageIds(target);
        for (TourAddonPriceItem sibling : siblings) {
            if (Objects.equals(sibling.getId(), target.getId())) {
                continue;
            }
            Set<Long> siblingPackageIds = parseAddonPackageIds(sibling);
            boolean sameScope;
            if (targetPackageIds.isEmpty() || siblingPackageIds.isEmpty()) {
                sameScope = targetPackageIds.isEmpty() && siblingPackageIds.isEmpty();
            } else {
                Set<Long> overlap = new LinkedHashSet<>(siblingPackageIds);
                overlap.retainAll(targetPackageIds);
                sameScope = !overlap.isEmpty();
            }
            if (!sameScope) {
                continue;
            }
            Set<Long> duplicate = parseBatchIds(sibling.getBatchIds());
            duplicate.retainAll(targetBatchIds);
            if (!duplicate.isEmpty()) {
                throw new ServiceException("同一附加费用、行程套餐和出发日期只能绑定一个启用价格项");
            }
        }
    }

    private boolean isPackagePriceItemMatched(TourPackagePriceItem item, Long tourId, Long packageId, Long batchId) {
        return item != null
                && Objects.equals(item.getTourId(), tourId)
                && Objects.equals(item.getPackageId(), packageId)
                && Integer.valueOf(1).equals(item.getStatus())
                && parseBatchIds(item.getBatchIds()).contains(batchId);
    }

    private boolean isAddonPriceItemMatched(TourAddonPriceItem item, Long tourId, Long addonId, Long batchId,
                                            Long packageId) {
        Set<Long> packageIds = parseAddonPackageIds(item);
        return item != null
                && Objects.equals(item.getTourId(), tourId)
                && Objects.equals(item.getAddonId(), addonId)
                && (packageIds.isEmpty() || packageIds.contains(packageId))
                && Integer.valueOf(1).equals(item.getStatus())
                && parseBatchIds(item.getBatchIds()).contains(batchId);
    }

    private void removeBatchIdFromPackageItems(Long batchId) {
        List<TourPackagePriceItem> items = packagePriceItemMapper.selectList(null);
        for (TourPackagePriceItem item : items) {
            Set<Long> batchIds = parseBatchIds(item.getBatchIds());
            if (batchIds.remove(batchId)) {
                item.setBatchIds(serializeBatchIds(batchIds));
                item.setUpdateTime(LocalDateTime.now());
                packagePriceItemMapper.updateById(item);
            }
        }
    }

    private void removeBatchIdFromAddonItems(Long batchId) {
        List<TourAddonPriceItem> items = addonPriceItemMapper.selectList(null);
        for (TourAddonPriceItem item : items) {
            Set<Long> batchIds = parseBatchIds(item.getBatchIds());
            if (batchIds.remove(batchId)) {
                item.setBatchIds(serializeBatchIds(batchIds));
                item.setUpdateTime(LocalDateTime.now());
                addonPriceItemMapper.updateById(item);
            }
        }
    }

    private BigDecimal positiveAmount(BigDecimal value, String message) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(message);
        }
        return amount;
    }

    private BigDecimal optionalPositiveAmount(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(message);
        }
        return value;
    }

    private BigDecimal validateOriginal(BigDecimal original, BigDecimal sale, String message) {
        if (original == null || sale == null) {
            return null;
        }
        if (original.compareTo(sale) <= 0) {
            throw new ServiceException(message);
        }
        return original;
    }

    public Map<Long, Set<Long>> packageBatchMap(Long tourId) {
        return getPackagePriceItemsByTourId(tourId).stream()
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                .collect(Collectors.groupingBy(
                        TourPackagePriceItem::getPackageId,
                        Collectors.flatMapping(item -> parseBatchIds(item.getBatchIds()).stream(), Collectors.toCollection(LinkedHashSet::new))
                ));
    }

    public Map<Long, Set<Long>> addonBatchMap(Long tourId) {
        return getAddonPriceItemsByTourId(tourId).stream()
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                .collect(Collectors.groupingBy(
                        TourAddonPriceItem::getAddonId,
                        Collectors.flatMapping(item -> parseBatchIds(item.getBatchIds()).stream(), Collectors.toCollection(LinkedHashSet::new))
                ));
    }
}
