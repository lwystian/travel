package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.FrequentTraveler;
import org.example.springboot.mapper.FrequentTravelerMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 常用出行人服务
 */
@Service
public class FrequentTravelerService extends ServiceImpl<FrequentTravelerMapper, FrequentTraveler> {

    /**
     * 获取用户的所有常用出行人
     */
    public List<FrequentTraveler> getByUserId(Long userId) {
        QueryWrapper<FrequentTraveler> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("is_default").orderByDesc("create_time");
        return list(queryWrapper);
    }

    /**
     * 保存或更新常用出行人
     */
    public FrequentTraveler saveOrUpdate(Long userId, FrequentTraveler traveler) {
        traveler.setUserId(userId);
        if (traveler.getIsDefault() == null) {
            traveler.setIsDefault(false);
        }
        if (traveler.getId() == null) {
            // 新增时设置为非默认
            traveler.setIsDefault(false);
            save(traveler);
        } else {
            // 更新时确保是当前用户的出行人
            QueryWrapper<FrequentTraveler> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", traveler.getId());
            queryWrapper.eq("user_id", userId);
            FrequentTraveler existing = getOne(queryWrapper);
            if (existing != null) {
                traveler.setUserId(userId);
                updateById(traveler);
            } else {
                return null;
            }
        }
        return traveler;
    }

    /**
     * 删除常用出行人
     */
    public boolean delete(Long userId, Long travelerId) {
        QueryWrapper<FrequentTraveler> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", travelerId);
        queryWrapper.eq("user_id", userId);
        return remove(queryWrapper);
    }

    /**
     * 设置默认出行人
     */
    public void setDefault(Long userId, Long travelerId) {
        // 先取消所有默认
        QueryWrapper<FrequentTraveler> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("user_id", userId);
        FrequentTraveler update = new FrequentTraveler();
        update.setIsDefault(false);
        update(updateWrapper);
        
        // 设置新的默认
        FrequentTraveler traveler = new FrequentTraveler();
        traveler.setId(travelerId);
        traveler.setIsDefault(true);
        updateById(traveler);
    }
}
