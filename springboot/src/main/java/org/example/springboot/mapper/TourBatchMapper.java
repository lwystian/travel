package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.springboot.entity.TourBatch;

@Mapper
public interface TourBatchMapper extends BaseMapper<TourBatch> {

    /**
     * 原子锁定库存（检查余位 + 增加occupied，一条SQL完成）
     * @param batchId 批次ID
     * @param totalPeople 需要锁定的人数
     * @return 影响行数：1=锁定成功，0=余位不足
     */
    @Update("UPDATE tour_batch " +
            "SET occupied = occupied + #{totalPeople}, " +
            "    update_time = NOW() " +
            "WHERE id = #{batchId} " +
            "  AND (remaining - occupied) >= #{totalPeople}")
    int lockOccupancy(@Param("batchId") Long batchId, @Param("totalPeople") int totalPeople);

    /**
     * 原子确认库存（支付时：将occupied转为remaining扣减）
     * @param batchId 批次ID
     * @param totalPeople 需要确认的人数
     * @return 影响行数：1=成功，0=锁定不足
     */
    @Update("UPDATE tour_batch " +
            "SET occupied = GREATEST(0, occupied - #{totalPeople}), " +
            "    remaining = GREATEST(0, remaining - #{totalPeople}), " +
            "    update_time = NOW() " +
            "WHERE id = #{batchId} " +
            "  AND occupied >= #{totalPeople}")
    int confirmOccupancy(@Param("batchId") Long batchId, @Param("totalPeople") int totalPeople);

    /**
     * 原子释放锁定库存（取消订单时：减少occupied）
     * @param batchId 批次ID
     * @param totalPeople 需要释放的人数
     * @return 影响行数：1=成功，0=锁定不足
     */
    @Update("UPDATE tour_batch " +
            "SET occupied = GREATEST(0, occupied - #{totalPeople}), " +
            "    update_time = NOW() " +
            "WHERE id = #{batchId} " +
            "  AND occupied >= #{totalPeople}")
    int releaseOccupancy(@Param("batchId") Long batchId, @Param("totalPeople") int totalPeople);

    /**
     * 原子退还余位（退款时：增加remaining）
     * @param batchId 批次ID
     * @param totalPeople 需要退还的人数
     * @param maxCapacity 最大容量限制
     * @return 影响行数
     */
    @Update("UPDATE tour_batch " +
            "SET remaining = LEAST(max_capacity, remaining + #{totalPeople}), " +
            "    update_time = NOW() " +
            "WHERE id = #{batchId}")
    int returnRemaining(@Param("batchId") Long batchId, @Param("totalPeople") int totalPeople, @Param("maxCapacity") int maxCapacity);
}
