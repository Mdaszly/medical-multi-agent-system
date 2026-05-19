package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.Icd10CodeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Icd10CodeMapper extends BaseMapper<Icd10CodeEntity> {
}
