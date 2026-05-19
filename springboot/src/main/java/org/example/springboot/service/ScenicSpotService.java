package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.ScenicCategory;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.ScenicTag;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.ScenicCategoryMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.ScenicTagMapper;
import org.example.springboot.security.SecurityValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScenicSpotService {
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    
    @Resource
    private ScenicCategoryService scenicCategoryService;
    
    @Autowired
    private ScenicCategoryMapper scenicCategoryMapper;
    
    @Resource
    private CommentMapper commentMapper;

    @Resource
    private ScenicTagMapper scenicTagMapper;

    @Autowired
    private ScenicTagService scenicTagService;

    public Page<ScenicSpot> getScenicSpotsByPage(String name, String location, Long categoryId, Integer currentPage, Integer size, String orderBy, String order) {
        currentPage = SecurityValidationUtil.clampPage(currentPage);
        size = SecurityValidationUtil.clampLimit(size, 10, 100);
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();

        // 如果有名称搜索，进行综合搜索（名称、地区、描述）
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.and(wrapper -> wrapper
                .like(ScenicSpot::getName, name)
                .or()
                .like(ScenicSpot::getLocation, name)
                .or()
                .like(ScenicSpot::getDescription, name)
            );
        }

        // 如果有单独的地区搜索
        if (StringUtils.isNotBlank(location)) {
            queryWrapper.like(ScenicSpot::getLocation, location);
        }

        // 分类筛选
        if (categoryId != null) {
            queryWrapper.eq(ScenicSpot::getCategoryId, categoryId);
        }

        // 排序处理
        if (StringUtils.isNotBlank(orderBy) && "rating".equals(orderBy)) {
            // 按评分排序需要先获取数据再排序，因为rating是计算字段
            queryWrapper.orderByDesc(ScenicSpot::getId); // 先按ID排序，后续再处理
        } else {
            // 默认按ID降序排序，让新添加的景点排在前面
            queryWrapper.orderByDesc(ScenicSpot::getId);
        }

        Page<ScenicSpot> page = scenicSpotMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充分类信息
        fillCategoryInfo(page.getRecords());
        // 填充评论信息（评分和评论数）
        fillReviewInfo(page.getRecords());
        // 填充标签信息
        fillTagsInfo(page.getRecords());

        // 如果是按评分排序，在内存中进行排序
        if (StringUtils.isNotBlank(orderBy) && "rating".equals(orderBy)) {
            List<ScenicSpot> records = page.getRecords();
            if ("desc".equalsIgnoreCase(order) || "asc".equalsIgnoreCase(order)) {
                boolean ascending = "asc".equalsIgnoreCase(order);
                records.sort((a, b) -> {
                    double ratingA = a.getRating() != null ? a.getRating() : 0;
                    double ratingB = b.getRating() != null ? b.getRating() : 0;
                    return ascending ? Double.compare(ratingA, ratingB) : Double.compare(ratingB, ratingA);
                });
            } else {
                records.sort((a, b) -> {
                    double ratingA = a.getRating() != null ? a.getRating() : 0;
                    double ratingB = b.getRating() != null ? b.getRating() : 0;
                    return Double.compare(ratingB, ratingA); // 默认降序
                });
            }
        }

        return page;
    }
    
    /**
     * 根据分类ID查询景点
     */
    public List<ScenicSpot> getScenicSpotsByCategoryId(Long categoryId) {
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicSpot::getCategoryId, categoryId);
        
        List<ScenicSpot> spots = scenicSpotMapper.selectList(queryWrapper);
        fillCategoryInfo(spots);
        fillReviewInfo(spots);
        fillTagsInfo(spots);
        
        return spots;
    }

    public ScenicSpot getById(Long id) {
        ScenicSpot spot = scenicSpotMapper.selectById(id);
        if (spot == null) throw new ServiceException("景点不存在");
        
        // 填充分类信息
        if (spot.getCategoryId() != null) {
            spot.setCategoryInfo(scenicCategoryService.getCategoryById(spot.getCategoryId()));
        }
        
        // 填充评论信息
        fillReviewInfo(List.of(spot));
        // 填充标签信息
        fillTagsInfo(List.of(spot));

        return spot;
    }

    public void createScenicSpot(ScenicSpot spot) {
        // 验证分类是否存在
        if (spot.getCategoryId() != null) {
            ScenicCategory category = scenicCategoryService.getCategoryById(spot.getCategoryId());
            if (category == null) {
                throw new ServiceException("所选分类不存在");
            }
        }
        
        if (scenicSpotMapper.insert(spot) <= 0) throw new ServiceException("新增景点失败");
    }

    public void updateScenicSpot(Long id, ScenicSpot spot) {
        if (scenicSpotMapper.selectById(id) == null) throw new ServiceException("景点不存在");
        spot.setId(id);
        
        // 验证分类是否存在
        if (spot.getCategoryId() != null) {
            ScenicCategory category = scenicCategoryService.getCategoryById(spot.getCategoryId());
            if (category == null) {
                throw new ServiceException("所选分类不存在");
            }
        }
        
        if (scenicSpotMapper.updateById(spot) <= 0) throw new ServiceException("更新景点失败");
    }

    public void deleteScenicSpot(Long id) {
        if (scenicSpotMapper.deleteById(id) <= 0) throw new ServiceException("删除景点失败");
    }

    public List<ScenicSpot> getAll() {
        List<ScenicSpot> spots = scenicSpotMapper.selectList(new LambdaQueryWrapper<>());
        fillCategoryInfo(spots);
        fillReviewInfo(spots);
        fillTagsInfo(spots);
        return spots;
    }
    
    /**
     * 填充景点的分类信息
     */
    private void fillCategoryInfo(List<ScenicSpot> spots) {
        if (spots == null || spots.isEmpty()) {
            return;
        }
        
        // 获取所有涉及到的分类ID
        List<Long> categoryIds = spots.stream()
                .map(ScenicSpot::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        if (categoryIds.isEmpty()) {
            return;
        }
        
        // 批量查询分类信息
        LambdaQueryWrapper<ScenicCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ScenicCategory::getId, categoryIds);
        List<ScenicCategory> categories = scenicCategoryMapper.selectList(queryWrapper);
        
        // 转换为Map便于查找
        Map<Long, ScenicCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(ScenicCategory::getId, category -> category));
        
        // 填充分类信息
        spots.forEach(spot -> {
            if (spot.getCategoryId() != null && categoryMap.containsKey(spot.getCategoryId())) {
                spot.setCategoryInfo(categoryMap.get(spot.getCategoryId()));
            }
        });
    }

    /**
     * 填充景点的评论信息（评分和评论数）
     */
    private void fillReviewInfo(List<ScenicSpot> spots) {
        if (spots == null || spots.isEmpty()) {
            return;
        }
        
        spots.forEach(spot -> {
            Long scenicId = spot.getId();
            if (scenicId != null) {
                // 查询该景点的所有评论
                LambdaQueryWrapper<Comment> commentQuery = new LambdaQueryWrapper<>();
                commentQuery.eq(Comment::getScenicId, scenicId);
                List<Comment> comments = commentMapper.selectList(commentQuery);
                
                if (comments != null && !comments.isEmpty()) {
                    // 计算平均评分
                    double avgRating = comments.stream()
                            .filter(c -> c.getRating() != null)
                            .mapToInt(Comment::getRating)
                            .average()
                            .orElse(0.0);
                    spot.setRating(Math.round(avgRating * 10.0) / 10.0); // 保留一位小数
                    spot.setReviewCount(comments.size());
                } else {
                    spot.setRating(0.0);
                    spot.setReviewCount(0);
                }
            }
        });
    }

    /**
     * 获取热门景点
     * @param limit 限制数量
     * @return 热门景点列表
     */
    public List<ScenicSpot> getHotScenics(Integer limit) {
        limit = SecurityValidationUtil.clampLimit(limit, 4, 50);
        // 这里可以根据实际需求定义热门景点的获取逻辑
        // 例如根据评分、访问量、价格等条件排序
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ScenicSpot::getId);
        queryWrapper.last("LIMIT " + limit);
        List<ScenicSpot> spots = scenicSpotMapper.selectList(queryWrapper);
        fillReviewInfo(spots);
        fillTagsInfo(spots);
        return spots;
    }

    /**
     * 根据ID列表批量查询景点
     * @param ids 景点ID列表
     * @return 景点列表
     */
    public List<ScenicSpot> getScenicSpotsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 直接从数据库查询，避免Redis缓存的类型转换问题
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ScenicSpot::getId, ids);
        List<ScenicSpot> spots = scenicSpotMapper.selectList(queryWrapper);

        // 填充分类信息
        fillCategoryInfo(spots);
        // 填充评论信息
        fillReviewInfo(spots);
        // 填充标签信息
        fillTagsInfo(spots);

        return spots;
    }

    /**
     * 获取搜索建议
     * @param keyword 搜索关键词
     * @param limit 限制数量
     * @return 搜索建议列表
     */
    public List<Map<String, Object>> getSearchSuggestions(String keyword, Integer limit) {
        limit = SecurityValidationUtil.clampLimit(limit, 5, 20);
        List<Map<String, Object>> result = new ArrayList<>();

        if (StringUtils.isBlank(keyword)) {
            return result;
        }

        // 搜索景点建议
        LambdaQueryWrapper<ScenicSpot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
            .like(ScenicSpot::getName, keyword)
            .or()
            .like(ScenicSpot::getLocation, keyword)
        );
        queryWrapper.orderByDesc(ScenicSpot::getId);
        queryWrapper.last("LIMIT " + limit);

        List<ScenicSpot> scenics = scenicSpotMapper.selectList(queryWrapper);

        for (ScenicSpot scenic : scenics) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("id", scenic.getId());
            suggestion.put("name", scenic.getName());
            suggestion.put("location", scenic.getLocation());
            suggestion.put("imageUrl", scenic.getImageUrl());
            suggestion.put("type", "scenic");
            result.add(suggestion);
        }

        return result;
    }

    /**
     * 填充景点的标签信息
     */
    private void fillTagsInfo(List<ScenicSpot> spots) {
        if (spots == null || spots.isEmpty()) {
            return;
        }

        spots.forEach(spot -> {
            if (spot.getId() != null) {
                List<ScenicTag> tags = scenicTagMapper.selectTagsByScenicSpotId(spot.getId());
                spot.setTags(tags);
            }
        });
    }

    /**
     * 保存景点的标签关联
     */
    public void saveScenicTags(Long scenicSpotId, List<Long> tagIds) {
        scenicTagService.saveScenicTags(scenicSpotId, tagIds);
    }
}
