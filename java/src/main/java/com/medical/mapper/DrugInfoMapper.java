package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.DrugInfo;
import com.medical.model.vo.DrugWithPriceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DrugInfoMapper extends BaseMapper<DrugInfo> {

    DrugInfo selectByDrugCode(@Param("drugCode") String drugCode);

    List<DrugWithPriceVO> selectDrugWithPriceList(@Param("drugCode") String drugCode, 
                                                  @Param("drugName") String drugName, 
                                                  @Param("categoryCode") String categoryCode,
                                                  @Param("status") Integer status);
}