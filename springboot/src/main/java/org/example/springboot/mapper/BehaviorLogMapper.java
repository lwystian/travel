package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.BehaviorLog;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BehaviorLogMapper extends BaseMapper<BehaviorLog> {
    List<BehaviorLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    List<BehaviorLog> selectByUserId(@Param("userId") Long userId);
}
