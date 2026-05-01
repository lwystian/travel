package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.TourBatch;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TourBatchMapper extends BaseMapper<TourBatch> {
}
