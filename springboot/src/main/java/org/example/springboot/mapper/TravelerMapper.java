package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Traveler;

@Mapper
public interface TravelerMapper extends BaseMapper<Traveler> {
}
