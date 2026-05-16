package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.PrescriptionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrescriptionItemMapper extends BaseMapper<PrescriptionItem> {

    List<PrescriptionItem> selectByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    int deleteByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}