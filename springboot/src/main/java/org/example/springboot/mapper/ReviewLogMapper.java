package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.ReviewLog;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReviewLogMapper extends BaseMapper<ReviewLog> {
    List<ReviewLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    List<ReviewLog> selectByTargetType(@Param("targetType") String targetType);
}
