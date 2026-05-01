package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.TourPackage;

import java.util.List;

@Mapper
public interface TourPackageMapper extends BaseMapper<TourPackage> {
}
