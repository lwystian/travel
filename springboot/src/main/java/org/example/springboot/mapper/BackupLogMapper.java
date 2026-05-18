package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.BackupLog;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BackupLogMapper extends BaseMapper<BackupLog> {
    List<BackupLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
