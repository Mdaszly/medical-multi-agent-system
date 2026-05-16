package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.entity.DrugPrice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DrugPriceMapper extends BaseMapper<DrugPrice> {

    BigDecimal selectCurrentPrice(@Param("drugId") Long drugId, @Param("priceType") String priceType);

    BigDecimal selectPriceByDate(@Param("drugId") Long drugId, @Param("priceType") String priceType, @Param("date") LocalDate date);

    DrugPrice selectCurrentPriceRecord(@Param("drugId") Long drugId, @Param("priceType") String priceType);

    int updateExpireDate(@Param("drugId") Long drugId, @Param("priceType") String priceType, @Param("expireDate") LocalDate expireDate);

    List<DrugPrice> selectPriceHistory(@Param("drugId") Long drugId, @Param("priceType") String priceType);
}