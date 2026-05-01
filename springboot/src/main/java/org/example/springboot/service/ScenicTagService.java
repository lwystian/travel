package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.springboot.entity.ScenicTag;
import org.example.springboot.entity.ScenicSpotTag;
import org.example.springboot.mapper.ScenicSpotTagMapper;
import org.example.springboot.mapper.ScenicTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScenicTagService extends ServiceImpl<ScenicTagMapper, ScenicTag> {
    
    @Resource
    private ScenicSpotTagMapper scenicSpotTagMapper;
    
    public List<ScenicTag> getTagsByScenicSpotId(Long scenicSpotId) {
        return baseMapper.selectTagsByScenicSpotId(scenicSpotId);
    }
    
    public List<ScenicTag> getAllTags() {
        QueryWrapper<ScenicTag> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order");
        return list(wrapper);
    }
    
    @Transactional
    public void saveScenicTags(Long scenicSpotId, List<Long> tagIds) {
        // 先删除原有关联
        QueryWrapper<ScenicSpotTag> wrapper = new QueryWrapper<>();
        wrapper.eq("scenic_spot_id", scenicSpotId);
        scenicSpotTagMapper.delete(wrapper);
        
        // 新增新关联
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                ScenicSpotTag spotTag = new ScenicSpotTag();
                spotTag.setScenicSpotId(scenicSpotId);
                spotTag.setTagId(tagId);
                scenicSpotTagMapper.insert(spotTag);
            }
        }
    }
    
    @Transactional
    public void deleteByScenicSpotId(Long scenicSpotId) {
        QueryWrapper<ScenicSpotTag> wrapper = new QueryWrapper<>();
        wrapper.eq("scenic_spot_id", scenicSpotId);
        scenicSpotTagMapper.delete(wrapper);
    }
}
