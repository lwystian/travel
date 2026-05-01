package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.ScenicSpotTag;

@Mapper
public interface ScenicSpotTagMapper extends BaseMapper<ScenicSpotTag> {
    
    @Select("SELECT tag_id FROM scenic_spot_tag WHERE scenic_spot_id = #{scenicSpotId}")
    java.util.List<Long> selectTagIdsByScenicSpotId(@Param("scenicSpotId") Long scenicSpotId);
}
