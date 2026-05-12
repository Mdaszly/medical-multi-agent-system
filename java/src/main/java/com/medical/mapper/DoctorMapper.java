package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
}