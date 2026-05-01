package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.DTO.TourOrderCreateDTO;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TourOrderService {

    private static final Logger logger = LoggerFactory.getLogger(TourOrderService.class);
    private static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.01");

    @Resource
    private TourOrderMapper tourOrderMapper;

    @Resource
    private TourMapper tourMapper;

    @Resource
    private TourPackageMapper tourPackageMapper;

    @Resource
    private BatchPackageMapper batchPackageMapper;

    @Resource
    private TourBatchMapper tourBatchMapper;

    /**
     * 创建行程订单
     */
    @Transactional
    public TourOrder createOrder(TourOrderCreateDTO dto) {
        // 1. 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }

        // 2. 根据行程编号查询行程
        LambdaQueryWrapper<Tour> tourWrapper = new LambdaQueryWrapper<>();
        tourWrapper.eq(Tour::getCode, dto.getProductId());
        Tour tour = tourMapper.selectOne(tourWrapper);
        if (tour == null) {
            throw new ServiceException("行程不存在");
        }
        if (tour.getStatus() != 1) {
            throw new ServiceException("该行程暂不可预订");
        }

        // 3. 验证套餐
        TourPackage tourPackage = tourPackageMapper.selectById(dto.getTripPackageId());
        if (tourPackage == null) {
            throw new ServiceException("套餐不存在");
        }
        if (!tourPackage.getTourId().equals(tour.getId())) {
            throw new ServiceException("套餐与行程不匹配");
        }
        if (tourPackage.getStatus() != 1) {
            throw new ServiceException("该套餐暂不可预订");
        }

        // 4. 验证出发日期批次
        LocalDate departureDate;
        TourBatch tourBatch;
        try {
            departureDate = LocalDate.parse(dto.getBatchDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new ServiceException("日期格式错误");
        }

        LambdaQueryWrapper<TourBatch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(TourBatch::getTourId, tour.getId())
                   .eq(TourBatch::getDepartureDate, departureDate);
        tourBatch = tourBatchMapper.selectOne(batchWrapper);
        if (tourBatch == null) {
            throw new ServiceException("该出发日期不存在");
        }
        if (!"可报名".equals(tourBatch.getStatus())) {
            throw new ServiceException("该批次" + tourBatch.getStatus() + "，不可预订");
        }

        // 5. 验证余位
        int totalPeople = dto.getAdultCount() + (dto.getChildCount() != null ? dto.getChildCount() : 0);
        if (tourBatch.getRemaining() < totalPeople) {
            throw new ServiceException("余位不足，当前剩余" + tourBatch.getRemaining() + "个名额");
        }

        // 6. 获取批次套餐及附加费
        BigDecimal batchExtraFee = BigDecimal.ZERO;
        String batchPackageName = "标准";
        if (dto.getBatchPackageId() != null) {
            BatchPackage batchPackage = batchPackageMapper.selectById(dto.getBatchPackageId());
            if (batchPackage != null && batchPackage.getTourId().equals(tour.getId())) {
                batchExtraFee = batchPackage.getExtraFeePerPerson() != null ? batchPackage.getExtraFeePerPerson() : BigDecimal.ZERO;
                batchPackageName = batchPackage.getName();
            }
        }

        // 7. 后端精确计算价格
        BigDecimal adultUnitPrice = calculateAdultUnitPrice(tourPackage, tourBatch, batchExtraFee);
        BigDecimal childUnitPrice = calculateChildUnitPrice(tourPackage, tourBatch, batchExtraFee);

        // 8. 验证前端传来的价格（允许0.01元误差）
        if (dto.getClientAdultUnitPrice() != null) {
            BigDecimal diff = adultUnitPrice.subtract(dto.getClientAdultUnitPrice()).abs();
            if (diff.compareTo(PRICE_TOLERANCE) > 0) {
                logger.warn("成人单价校验失败：前端={}, 后端={}, 差异={}",
                    dto.getClientAdultUnitPrice(), adultUnitPrice, diff);
                throw new ServiceException("价格已变更，请刷新页面后重试");
            }
        }

        if (dto.getClientChildUnitPrice() != null && dto.getClientChildUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = childUnitPrice.subtract(dto.getClientChildUnitPrice()).abs();
            if (diff.compareTo(PRICE_TOLERANCE) > 0) {
                logger.warn("儿童单价校验失败：前端={}, 后端={}, 差异={}",
                    dto.getClientChildUnitPrice(), childUnitPrice, diff);
                throw new ServiceException("价格已变更，请刷新页面后重试");
            }
        }

        // 9. 计算行程费用
        BigDecimal tourAmount = adultUnitPrice.multiply(new BigDecimal(dto.getAdultCount()));
        if (dto.getChildCount() != null && dto.getChildCount() > 0) {
            tourAmount = tourAmount.add(childUnitPrice.multiply(new BigDecimal(dto.getChildCount())));
        }

        // 10. 计算酒店费用（如有）
        BigDecimal hotelAmount = BigDecimal.ZERO;
        if (dto.getHotelId() != null && dto.getHotelDays() != null && dto.getHotelDays() > 0) {
            if (dto.getHotelPricePerNight() == null || dto.getHotelPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("酒店价格信息错误");
            }
            hotelAmount = dto.getHotelPricePerNight().multiply(new BigDecimal(dto.getHotelDays()));
        }

        // 11. 计算订单总金额
        BigDecimal totalAmount = tourAmount.add(hotelAmount);

        // 12. 验证前端传来的总价（允许0.01元误差）
        if (dto.getClientTotalPrice() != null) {
            BigDecimal diff = totalAmount.subtract(dto.getClientTotalPrice()).abs();
            if (diff.compareTo(PRICE_TOLERANCE) > 0) {
                logger.warn("总价校验失败：前端={}, 后端={}, 差异={}",
                    dto.getClientTotalPrice(), totalAmount, diff);
                throw new ServiceException("价格已变更，请刷新页面后重试");
            }
        }

        // 13. 创建订单
        TourOrder order = new TourOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(currentUser.getId());
        order.setTourId(tour.getId());
        order.setTourName(tour.getTitle());
        order.setTourCode(tour.getCode());
        order.setPackageId(tourPackage.getId());
        order.setPackageName(tourPackage.getName());
        order.setBatchPackageId(dto.getBatchPackageId());
        order.setBatchPackageName(batchPackageName);
        order.setDepartureDate(departureDate);
        order.setAdultCount(dto.getAdultCount());
        order.setChildCount(dto.getChildCount() != null ? dto.getChildCount() : 0);
        order.setAdultUnitPrice(adultUnitPrice);
        order.setChildUnitPrice(childUnitPrice);
        order.setTourAmount(tourAmount);
        order.setHotelId(dto.getHotelId());
        order.setHotelName(dto.getHotelName());
        order.setHotelDays(dto.getHotelDays());
        order.setHotelPricePerNight(dto.getHotelPricePerNight());
        order.setHotelAmount(hotelAmount);
        order.setTotalAmount(totalAmount);
        order.setContactName(dto.getContactName());
        order.setContactPhone(dto.getContactPhone());
        order.setRemark(dto.getRemark());
        order.setStatus(0); // 待支付
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 14. 保存订单
        tourOrderMapper.insert(order);

        // 15. 扣减余位
        tourBatch.setRemaining(tourBatch.getRemaining() - totalPeople);
        tourBatchMapper.updateById(tourBatch);

        logger.info("行程订单创建成功：订单号={}, 用户={}, 行程={}, 总价={}",
            order.getOrderNo(), currentUser.getUsername(), tour.getTitle(), totalAmount);

        return order;
    }

    /**
     * 计算成人单价：套餐价 + 日期附加费 + 批次附加费
     */
    private BigDecimal calculateAdultUnitPrice(TourPackage tourPackage, TourBatch tourBatch, BigDecimal batchExtraFee) {
        BigDecimal price = tourPackage.getAdultPrice() != null ? tourPackage.getAdultPrice() : BigDecimal.ZERO;
        if (tourBatch.getAdultDateExtraFee() != null) {
            price = price.add(tourBatch.getAdultDateExtraFee());
        }
        price = price.add(batchExtraFee);
        return price;
    }

    /**
     * 计算儿童单价：套餐儿童价 + 日期附加费 + 批次附加费
     */
    private BigDecimal calculateChildUnitPrice(TourPackage tourPackage, TourBatch tourBatch, BigDecimal batchExtraFee) {
        BigDecimal childPrice = tourPackage.getChildPrice();
        if (childPrice == null || childPrice.compareTo(BigDecimal.ZERO) == 0) {
            // 如果没有儿童价，返回0
            return BigDecimal.ZERO;
        }
        BigDecimal price = childPrice;
        if (tourBatch.getChildDateExtraFee() != null) {
            price = price.add(tourBatch.getChildDateExtraFee());
        }
        price = price.add(batchExtraFee);
        return price;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
        return "TR" + dateStr + randomStr;
    }

    /**
     * 支付订单
     */
    @Transactional
    public void payOrder(Long orderId, String paymentMethod) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态不正确，无法支付");
        }

        order.setStatus(1); // 已支付
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        logger.info("行程订单支付成功：订单号={}", order.getOrderNo());
    }

    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付的订单可以取消");
        }

        // 恢复余位
        restoreBatchRemaining(order);

        order.setStatus(2); // 已取消
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        logger.info("行程订单取消成功：订单号={}", order.getOrderNo());
    }

    /**
     * 退款订单
     */
    @Transactional
    public void refundOrder(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("只有管理员可以执行退款操作");
        }

        if (order.getStatus() != 1) {
            throw new ServiceException("只有已支付的订单可以退款");
        }

        // 恢复余位
        restoreBatchRemaining(order);

        order.setStatus(3); // 已退款
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        logger.info("行程订单退款成功：订单号={}", order.getOrderNo());
    }

    /**
     * 恢复批次余位
     */
    private void restoreBatchRemaining(TourOrder order) {
        LambdaQueryWrapper<TourBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourBatch::getTourId, order.getTourId())
               .eq(TourBatch::getDepartureDate, order.getDepartureDate());
        TourBatch batch = tourBatchMapper.selectOne(wrapper);
        if (batch != null) {
            int totalPeople = order.getAdultCount() + (order.getChildCount() != null ? order.getChildCount() : 0);
            batch.setRemaining(batch.getRemaining() + totalPeople);
            tourBatchMapper.updateById(batch);
        }
    }

    /**
     * 获取用户订单列表
     */
    public Page<TourOrder> getUserOrders(Integer status, Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }

        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TourOrder::getUserId, currentUser.getId());

        if (status != null) {
            wrapper.eq(TourOrder::getStatus, status);
        }

        wrapper.orderByDesc(TourOrder::getCreateTime);

        return tourOrderMapper.selectPage(new Page<>(currentPage, size), wrapper);
    }

    /**
     * 获取订单详情
     */
    public TourOrder getOrderDetail(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权查看此订单");
        }

        return order;
    }

    /**
     * 管理员获取所有订单
     */
    public Page<TourOrder> getAllOrders(String orderNo, String contactName, String contactPhone,
                                        Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(orderNo)) {
            wrapper.like(TourOrder::getOrderNo, orderNo);
        }
        if (StringUtils.isNotBlank(contactName)) {
            wrapper.like(TourOrder::getContactName, contactName);
        }
        if (StringUtils.isNotBlank(contactPhone)) {
            wrapper.like(TourOrder::getContactPhone, contactPhone);
        }
        if (status != null) {
            wrapper.eq(TourOrder::getStatus, status);
        }

        wrapper.orderByDesc(TourOrder::getCreateTime);

        return tourOrderMapper.selectPage(new Page<>(currentPage, size), wrapper);
    }

    /**
     * 获取用户订单统计信息
     */
    public Map<String, Object> getUserOrderStats(Long userId) {
        LambdaQueryWrapper<TourOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TourOrder::getUserId, userId);
        
        List<TourOrder> allOrders = tourOrderMapper.selectList(queryWrapper);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allOrders.size());
        stats.put("pending", 0);
        stats.put("paid", 0);
        stats.put("cancelled", 0);
        stats.put("refunded", 0);
        stats.put("completed", 0);
        
        for (TourOrder order : allOrders) {
            switch (order.getStatus()) {
                case 0:
                    stats.put("pending", (Integer) stats.get("pending") + 1);
                    break;
                case 1:
                    stats.put("paid", (Integer) stats.get("paid") + 1);
                    break;
                case 2:
                    stats.put("cancelled", (Integer) stats.get("cancelled") + 1);
                    break;
                case 3:
                    stats.put("refunded", (Integer) stats.get("refunded") + 1);
                    break;
                case 4:
                    stats.put("completed", (Integer) stats.get("completed") + 1);
                    break;
            }
        }
        
        return stats;
    }

    /**
     * 删除订单（管理员）
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        // 只有已取消、已退款、已完成的订单可以删除
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != 4) {
            throw new ServiceException("只有已取消、已退款、已完成的订单可以删除");
        }

        tourOrderMapper.deleteById(orderId);
        logger.info("行程订单删除成功：订单号={}", order.getOrderNo());
    }

    /**
     * 更新订单联系人信息
     */
    @Transactional
    public void updateContactInfo(Long orderId, String contactName, String contactPhone) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付的订单可以修改联系人信息");
        }

        order.setContactName(contactName);
        order.setContactPhone(contactPhone);
        order.setUpdateTime(LocalDateTime.now());

        tourOrderMapper.updateById(order);
        logger.info("订单联系人信息更新成功：订单号={}", order.getOrderNo());
    }
}
