package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.springboot.entity.FrequentTraveler;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.FrequentTravelerMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 常用出行人服务
 */
@Service
public class FrequentTravelerService extends ServiceImpl<FrequentTravelerMapper, FrequentTraveler> {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-Za-z0-9]{5,20}$");
    private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] ID_CARD_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    @Resource
    private SiteNotificationService siteNotificationService;

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
        validateTraveler(traveler);
        traveler.setUserId(userId);
        if (traveler.getIsDefault() == null) {
            traveler.setIsDefault(false);
        }
        if (traveler.getId() == null) {
            // 新增时设置为非默认
            traveler.setIsDefault(false);
            save(traveler);
            sendTravelerNotification(userId, "常用出行人已添加",
                    "你刚刚添加了常用出行人「" + safeName(traveler.getName()) + "」，预订行程时可直接选择使用。",
                    traveler.getId());
        } else {
            // 更新时确保是当前用户的出行人
            QueryWrapper<FrequentTraveler> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", traveler.getId());
            queryWrapper.eq("user_id", userId);
            FrequentTraveler existing = getOne(queryWrapper);
            if (existing != null) {
                traveler.setUserId(userId);
                updateById(traveler);
                sendTravelerNotification(userId, "常用出行人已更新",
                        "常用出行人「" + safeName(traveler.getName()) + "」的信息刚刚完成更新。",
                        traveler.getId());
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
        FrequentTraveler traveler = getOne(queryWrapper);
        if (traveler == null) {
            return false;
        }
        boolean removed = remove(queryWrapper);
        if (removed) {
            sendTravelerNotification(userId, "常用出行人已删除",
                    "常用出行人「" + safeName(traveler.getName()) + "」已从你的资料中删除。",
                    travelerId);
        }
        return removed;
    }

    /**
     * 设置默认出行人
     */
    public boolean setDefault(Long userId, Long travelerId) {
        QueryWrapper<FrequentTraveler> ownerWrapper = new QueryWrapper<>();
        ownerWrapper.eq("id", travelerId);
        ownerWrapper.eq("user_id", userId);
        FrequentTraveler existing = getOne(ownerWrapper);
        if (existing == null) {
            return false;
        }

        // 先取消所有默认
        QueryWrapper<FrequentTraveler> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("user_id", userId);
        FrequentTraveler update = new FrequentTraveler();
        update.setIsDefault(false);
        update(updateWrapper);
        
        // 设置新的默认
        FrequentTraveler traveler = new FrequentTraveler();
        traveler.setId(travelerId);
        traveler.setUserId(userId);
        traveler.setIsDefault(true);
        boolean updated = update(traveler, ownerWrapper);
        if (updated) {
            sendTravelerNotification(userId, "默认出行人已更新",
                    "已将「" + safeName(existing.getName()) + "」设为默认出行人。",
                    travelerId);
        }
        return updated;
    }

    private void sendTravelerNotification(Long userId, String title, String content, Long travelerId) {
        siteNotificationService.sendToUser(userId, title, content,
                "ACCOUNT", "FREQUENT_TRAVELER", String.valueOf(travelerId), "/profile?tab=travelers");
    }

    private void validateTraveler(FrequentTraveler traveler) {
        if (traveler == null) {
            throw new ServiceException("出行人信息不能为空");
        }
        if (isBlank(traveler.getName()) || traveler.getName().trim().length() < 2 || traveler.getName().trim().length() > 20) {
            throw new ServiceException("出行人姓名长度需为 2-20 个字符");
        }
        if (isBlank(traveler.getPhone()) || !PHONE_PATTERN.matcher(traveler.getPhone().trim()).matches()) {
            throw new ServiceException("请输入正确的 11 位手机号码");
        }
        if (!"MALE".equals(traveler.getGender()) && !"FEMALE".equals(traveler.getGender())) {
            throw new ServiceException("性别参数不正确");
        }
        if (!"ADULT".equals(traveler.getTravelerType()) && !"CHILD".equals(traveler.getTravelerType())) {
            throw new ServiceException("出行人类型不正确");
        }
        if (!"ID_CARD".equals(traveler.getIdType()) && !"PASSPORT".equals(traveler.getIdType())) {
            throw new ServiceException("证件类型不正确");
        }
        if (traveler.getBirthDate() == null) {
            throw new ServiceException("请选择出生日期");
        }
        String idNumber = traveler.getIdNumber() == null ? "" : traveler.getIdNumber().trim();
        traveler.setIdNumber(idNumber);
        if ("ID_CARD".equals(traveler.getIdType()) && !isValidIdCard(idNumber)) {
            throw new ServiceException("请输入有效的 18 位身份证号码");
        }
        if ("PASSPORT".equals(traveler.getIdType()) && !PASSPORT_PATTERN.matcher(idNumber).matches()) {
            throw new ServiceException("护照号码需为 5-20 位字母或数字");
        }
        traveler.setName(traveler.getName().trim());
        traveler.setPhone(traveler.getPhone().trim());
    }

    private boolean isValidIdCard(String idNumber) {
        if (!ID_CARD_PATTERN.matcher(idNumber).matches()) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Character.digit(idNumber.charAt(i), 10) * ID_CARD_WEIGHTS[i];
        }
        return ID_CARD_CHECK_CODES[sum % 11] == Character.toUpperCase(idNumber.charAt(17));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeName(String name) {
        return name == null || name.isBlank() ? "未命名出行人" : name.trim();
    }
}
