package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.HealthProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HealthProfileMapper extends BaseMapper<HealthProfile> {

    @Select("SELECT * FROM health_profile WHERE user_id = #{userId}")
    HealthProfile selectByUserId(@Param("userId") Long userId);
}