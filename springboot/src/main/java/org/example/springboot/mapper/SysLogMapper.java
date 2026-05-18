package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.SysLog;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
    List<SysLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    int deleteBeforeTime(@Param("time") LocalDateTime time);
}
