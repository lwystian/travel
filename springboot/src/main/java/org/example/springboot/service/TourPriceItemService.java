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
                .orderByAsc(TourAddonPriceItem::getSortOrder)
                .orderByAsc(TourAddonPriceItem::getId));
    }

    public List<TourAddonPriceItem> getAddonPriceItems(Long addonId) {
        if (addonId == null) {
            return List.of();
        }
        return addonPriceItemMapper.selectList(new LambdaQueryWrapper<TourAddonPriceItem>()
                .eq(TourAddonPriceItem::getAddonId, addonId)
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

    public TourAddonPriceItem resolveAddonPriceItem(Long tourId, Long addonId, Long batchId, Long requestedPriceItemId) {
        if (tourId == null || addonId == null || batchId == null) {
            return null;
        }
        if (requestedPriceItemId != null) {
            TourAddonPriceItem item = addonPriceItemMapper.selectById(requestedPriceItemId);
            if (!isAddonPriceItemMatched(item, tourId, addonId, batchId)) {
                throw new ServiceException("附加费用价格项与当前出发日期不匹配");
            }
            return item;
        }
        List<TourAddonPriceItem> matchedItems = getAddonPriceItems(addonId).stream()
                .filter(item -> isAddonPriceItemMatched(item, tourId, addonId, batchId))
                .toList();
        if (matchedItems.size() > 1) {
            throw new ServiceException("该附加费用在当前出发日期存在多个启用价格项，请联系管理员处理");
        }
        return matchedItems.isEmpty() ? null : matchedItems.get(0);
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
        BigDecimal childPrice = positiveAmount(source.getChildPrice(), "儿童售价必须大于0");
        item.setChildPrice(childPrice);
        item.setOriginalAdultPrice(validateOriginal(source.getOriginalAdultPrice(), adultPrice, "成人划线价必须高于成人售价"));
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
        item.setName(StringUtils.defaultIfBlank(source.getName(), "价格项"));
        BigDecimal price = positiveAmount(source.getPrice(), "附加费用售价必须大于0");
        item.setPrice(price);
        item.setOriginalPrice(null);
        item.setBatchIds(serializeBatchIds(requireTourBatchIds(addon.getTourId(), parseBatchIds(source.getBatchIds()))));
        item.setStatus(source.getStatus() == null ? 1 : source.getStatus());
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
        for (TourAddonPriceItem sibling : siblings) {
            if (Objects.equals(sibling.getId(), target.getId())) {
                continue;
            }
            Set<Long> duplicate = parseBatchIds(sibling.getBatchIds());
            duplicate.retainAll(targetBatchIds);
            if (!duplicate.isEmpty()) {
                throw new ServiceException("同一附加费用的同一出发日期只能绑定一个启用价格项");
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

    private boolean isAddonPriceItemMatched(TourAddonPriceItem item, Long tourId, Long addonId, Long batchId) {
        return item != null
                && Objects.equals(item.getTourId(), tourId)
                && Objects.equals(item.getAddonId(), addonId)
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
