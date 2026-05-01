package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.Tour;

import java.util.List;

@Mapper
public interface TourMapper extends BaseMapper<Tour> {
    // 无需实现任何方法，使用MyBatisPlus提供的方法即可
}
