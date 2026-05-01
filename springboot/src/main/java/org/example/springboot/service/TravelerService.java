package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.Traveler;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TravelerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 出行人信息服务
 */
@Service
public class TravelerService extends ServiceImpl<TravelerMapper, Traveler> {

    /**
     * 根据订单ID获取出行人列表
     */
    public List<Traveler> getByOrderId(Long orderId) {
        LambdaQueryWrapper<Traveler> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Traveler::getOrderId, orderId).orderByAsc(Traveler::getTravelerIndex);
        return list(wrapper);
    }

    /**
     * 根据订单号获取出行人列表
     */
    public List<Traveler> getByOrderNo(String orderNo) {
        LambdaQueryWrapper<Traveler> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Traveler::getOrderNo, orderNo).orderByAsc(Traveler::getTravelerIndex);
        return list(wrapper);
    }

    /**
     * 批量保存出行人信息
     */
    @Transactional
    public void saveBatch(Long orderId, String orderNo, List<Traveler> travelers) {
        // 删除旧的出行人信息
        LambdaQueryWrapper<Traveler> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Traveler::getOrderId, orderId);
        remove(wrapper);

        // 保存新的出行人信息
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < travelers.size(); i++) {
            Traveler traveler = travelers.get(i);
            traveler.setOrderId(orderId);
            traveler.setOrderNo(orderNo);
            traveler.setTravelerIndex(i + 1);
            traveler.setCreateTime(now);
            traveler.setUpdateTime(now);
        }
        saveBatch(travelers);
    }

    /**
     * 验证出行人信息完整性
     */
    public void validateTravelers(List<Traveler> travelers, int expectedAdultCount, int expectedChildCount) {
        if (travelers == null || travelers.isEmpty()) {
            throw new ServiceException("请填写所有出行人信息");
        }

        int adultCount = 0;
        int childCount = 0;

        for (int i = 0; i < travelers.size(); i++) {
            Traveler t = travelers.get(i);
            int index = i + 1;

            if (t.getName() == null || t.getName().trim().isEmpty()) {
                throw new ServiceException("第" + index + "位出行人姓名为空");
            }
            if (t.getIdType() == null || t.getIdType().trim().isEmpty()) {
                throw new ServiceException("第" + index + "位出行人证件类型为空");
            }
            if (t.getIdNumber() == null || t.getIdNumber().trim().isEmpty()) {
                throw new ServiceException("第" + index + "位出行人证件号码为空");
            }
            if (t.getBirthDate() == null) {
                throw new ServiceException("第" + index + "位出行人出生日期为空");
            }
            if (t.getGender() == null || t.getGender().trim().isEmpty()) {
                throw new ServiceException("第" + index + "位出行人性别为空");
            }
            if (t.getTravelerType() == null || t.getTravelerType().trim().isEmpty()) {
                throw new ServiceException("第" + index + "位出行人类型为空");
            }
            if (t.getPhone() == null || t.getPhone().trim().isEmpty()) {
                throw new ServiceException("第" + index + "位出行人手机号码为空");
            }

            if ("ADULT".equals(t.getTravelerType())) {
                adultCount++;
            } else if ("CHILD".equals(t.getTravelerType())) {
                childCount++;
            }
        }

        if (adultCount != expectedAdultCount) {
            throw new ServiceException("成人数量不匹配，需要" + expectedAdultCount + "位成人");
        }
        if (childCount != expectedChildCount) {
            throw new ServiceException("儿童数量不匹配，需要" + expectedChildCount + "位儿童");
        }
    }

    /**
     * 根据出行人类型生成默认列表
     */
    public List<Traveler> generateDefaultTravelers(int adultCount, int childCount) {
        List<Traveler> travelers = new ArrayList<>();

        for (int i = 0; i < adultCount; i++) {
            Traveler adult = new Traveler();
            adult.setTravelerType("ADULT");
            adult.setIdType("ID_CARD");
            travelers.add(adult);
        }

        for (int i = 0; i < childCount; i++) {
            Traveler child = new Traveler();
            child.setTravelerType("CHILD");
            child.setIdType("ID_CARD");
            travelers.add(child);
        }

        return travelers;
    }
}
