package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.SensitiveWord;

@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
}
