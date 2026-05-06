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
}
