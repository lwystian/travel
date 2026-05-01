package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.springboot.entity.ScenicTag;

import java.util.List;

@Mapper
public interface ScenicTagMapper extends BaseMapper<ScenicTag> {
    
    @Select("SELECT t.* FROM scenic_tag t " +
            "INNER JOIN scenic_spot_tag st ON t.id = st.tag_id " +
            "WHERE st.scenic_spot_id = #{scenicSpotId} " +
            "ORDER BY t.sort_order ASC")
    List<ScenicTag> selectTagsByScenicSpotId(@Param("scenicSpotId") Long scenicSpotId);
}
