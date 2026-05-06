package org.example.springboot.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springboot.entity.TourBatch;
import org.example.springboot.entity.TourOrder;
import org.example.springboot.mapper.TourBatchMapper;
import org.example.springboot.mapper.TourOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单定时任务
 * 用于处理订单超时自动取消等任务
 */
@Component
public class OrderTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderTask.class);

    /**
     * 订单超时时间（分钟）
     */
    private static final int ORDER_TIMEOUT_MINUTES = 30;

    @Autowired
    private TourOrderMapper tourOrderMapper;

    @Autowired
    private TourBatchMapper tourBatchMapper;

    /**
     * 定时取消超时未支付的订单
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    @Transactional
    public void cancelExpiredOrders() {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);

            // 查询超时未支付的订单（状态为0）
            LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TourOrder::getStatus, 0)  // 0 = 待支付
                   .lt(TourOrder::getCreateTime, expireTime);  // 创建时间早于超时时间

            var expiredOrders = tourOrderMapper.selectList(wrapper);

            if (expiredOrders.isEmpty()) {
                return;
            }

            logger.info("发现 {} 个超时未支付的订单", expiredOrders.size());

            for (TourOrder order : expiredOrders) {
                // 释放锁定库存
                releaseBatchOccupancy(order);

                order.setStatus(2);  // 2 = 已取消
                order.setRemark("系统自动取消：支付超时" + ORDER_TIMEOUT_MINUTES + "分钟");
                order.setUpdateTime(LocalDateTime.now());
                tourOrderMapper.updateById(order);
                logger.info("自动取消超时订单: {}, 创建时间: {}", order.getOrderNo(), order.getCreateTime());
            }

            logger.info("订单超时取消任务执行完成，共取消 {} 个订单", expiredOrders.size());

        } catch (Exception e) {
            logger.error("执行订单超时取消任务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 释放锁定库存（超时取消订单时调用）
     */
    private void releaseBatchOccupancy(TourOrder order) {
        try {
            LambdaQueryWrapper<TourBatch> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TourBatch::getTourId, order.getTourId())
                   .eq(TourBatch::getDepartureDate, order.getDepartureDate());
            TourBatch batch = tourBatchMapper.selectOne(wrapper);
            if (batch != null) {
                int totalPeople = order.getAdultCount() + (order.getChildCount() != null ? order.getChildCount() : 0);
                int currentOccupied = batch.getOccupied() != null ? batch.getOccupied() : 0;
                batch.setOccupied(Math.max(0, currentOccupied - totalPeople));
                tourBatchMapper.updateById(batch);
                logger.info("超时取消释放锁定库存：行程={}, 日期={}, 人数={}, 当前锁定={}",
                    order.getTourId(), order.getDepartureDate(), totalPeople, batch.getOccupied());
            }
        } catch (Exception e) {
            logger.error("释放锁定库存失败：订单号={}, 错误={}", order.getOrderNo(), e.getMessage(), e);
        }
    }

    /**
     * 清理过期的内存缓存（每小时执行一次）
     * 主要用于清理 notify_id 缓存
     */
    @Scheduled(fixedRate = 3600000) // 每60分钟执行一次
    public void cleanupMemoryCache() {
        logger.debug("定时任务：内存缓存检查（可扩展用于其他清理任务）");
    }
}
