package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.entity.Traveler;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.TourOrderMapper;
import org.example.springboot.mapper.TravelerMapper;
import org.example.springboot.security.RolePermission;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 出行人信息服务
 */
@Service
public class TravelerService extends ServiceImpl<TravelerMapper, Traveler> {
    @Resource
    private TourOrderMapper tourOrderMapper;
    @Resource
    private AdminPermissionService adminPermissionService;

    /**
     * 根据订单ID获取出行人列表
     */
    public List<Traveler> getByOrderId(Long orderId) {
        User currentUser = requireOrderAccess(orderId);
        LambdaQueryWrapper<Traveler> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Traveler::getOrderId, orderId).orderByAsc(Traveler::getTravelerIndex);
        List<Traveler> travelers = list(wrapper);
        return canManageOrders(currentUser) ? travelers : maskTravelersForUser(travelers);
    }

    /**
     * 根据订单号获取出行人列表
     */
    public List<Traveler> getByOrderNo(String orderNo) {
        LambdaQueryWrapper<Traveler> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Traveler::getOrderNo, orderNo).orderByAsc(Traveler::getTravelerIndex);
        List<Traveler> travelers = list(wrapper);
        if (travelers.isEmpty()) {
            return travelers;
        }
        User currentUser = requireOrderAccess(travelers.get(0).getOrderId());
        return canManageOrders(currentUser) ? travelers : maskTravelersForUser(travelers);
    }

    /**
     * 批量保存出行人信息
     */
    @Transactional
    public void saveBatch(Long orderId, String orderNo, List<Traveler> travelers) {
        TourOrder order = requireEditableOrderAccess(orderId);
        // 删除旧的出行人信息
        LambdaQueryWrapper<Traveler> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Traveler::getOrderId, orderId);
        remove(wrapper);

        // 保存新的出行人信息
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < travelers.size(); i++) {
            Traveler traveler = travelers.get(i);
            traveler.setOrderId(orderId);
            traveler.setOrderNo(order.getOrderNo());
            traveler.setTravelerIndex(i + 1);
            traveler.setCreateTime(now);
            traveler.setUpdateTime(now);
        }
        saveBatch(travelers);
    }

    private TourOrder requireEditableOrderAccess(Long orderId) {
        TourOrder order = requireOrder(orderId);
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !canManageOrders(currentUser)) {
            throw new ServiceException("无权修改此订单出行人信息");
        }
        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付订单可以修改出行人信息");
        }
        return order;
    }

    private User requireOrderAccess(Long orderId) {
        TourOrder order = requireOrder(orderId);
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !canManageOrders(currentUser)) {
            throw new ServiceException("无权查看此订单出行人信息");
        }
        return currentUser;
    }

    private boolean canManageOrders(User currentUser) {
        return RolePermission.isAdmin(currentUser)
                && (RolePermission.isSuperAdmin(currentUser)
                || adminPermissionService.hasPermission(currentUser, "order:manage"));
    }

    private TourOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new ServiceException("订单不存在");
        }
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        return order;
    }

    private List<Traveler> maskTravelersForUser(List<Traveler> travelers) {
        return travelers.stream().map(this::copyMaskedTravelerForUser).toList();
    }

    private Traveler copyMaskedTravelerForUser(Traveler source) {
        Traveler copy = new Traveler();
        copy.setId(source.getId());
        copy.setOrderId(source.getOrderId());
        copy.setOrderNo(source.getOrderNo());
        copy.setName(source.getName());
        copy.setIdType(source.getIdType());
        copy.setIdNumber(maskIdNumberForUser(source.getIdNumber()));
        copy.setBirthDate(source.getBirthDate());
        copy.setGender(source.getGender());
        copy.setTravelerType(source.getTravelerType());
        copy.setPhone(maskPhoneForUser(source.getPhone()));
        copy.setTravelerIndex(source.getTravelerIndex());
        copy.setCreateTime(source.getCreateTime());
        copy.setUpdateTime(source.getUpdateTime());
        return copy;
    }

    private String maskPhoneForUser(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String value = phone.trim();
        if (value.matches("^1[3-9]\\d{9}$")) {
            return value.replaceAll("^(\\d{3})\\d{4}(\\d{4})$", "$1****$2");
        }
        if (value.length() <= 4) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private String maskIdNumberForUser(String idNumber) {
        if (!StringUtils.hasText(idNumber)) {
            return idNumber;
        }
        String value = idNumber.trim();
        if (value.length() <= 8) {
            return value.substring(0, 1) + "****" + value.substring(value.length() - 1);
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
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
