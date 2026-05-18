package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.SensitiveLog;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SensitiveLogMapper extends BaseMapper<SensitiveLog> {
    List<SensitiveLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    List<SensitiveLog> selectByUserId(@Param("userId") Long userId);
}
