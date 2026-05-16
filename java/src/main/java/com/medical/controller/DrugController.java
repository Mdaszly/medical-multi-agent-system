package com.medical.controller;

import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.drug.DrugAddRequest;
import com.medical.model.dto.drug.DrugQueryRequest;
import com.medical.model.dto.drug.DrugUpdateRequest;
import com.medical.model.dto.drug.FeeCalculationRequest;
import com.medical.model.dto.drug.PriceAddRequest;
import com.medical.model.vo.DrugVO;
import com.medical.model.vo.DrugWithPriceVO;
import com.medical.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/drug")
@RequiredArgsConstructor
@Tag(name = "药品管理", description = "药品信息管理和费用计算接口")
public class DrugController {

    private final DrugService drugService;

    /**
     * 新增药品
     *
     * <p>权限说明：仅管理员可操作
     */
    @PostMapping("/add")
    @Operation(summary = "新增药品", description = "新增药品信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DrugVO> addDrug(@RequestBody DrugAddRequest request) {
        DrugVO vo = drugService.addDrug(request);
        return ResultUtils.success(vo);
    }

    /**
     * 更新药品
     *
     * <p>权限说明：仅管理员可操作
     */
    @PostMapping("/update")
    @Operation(summary = "更新药品", description = "更新药品信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DrugVO> updateDrug(@RequestBody DrugUpdateRequest request) {
        DrugVO vo = drugService.updateDrug(request);
        return ResultUtils.success(vo);
    }

    /**
     * 删除药品
     *
     * <p>权限说明：仅管理员可操作
     */
    @PostMapping("/delete")
    @Operation(summary = "删除药品", description = "删除药品（软删除）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> deleteDrug(@Parameter(description = "药品ID") @RequestParam Long id) {
        drugService.deleteDrug(id);
        return ResultUtils.success( null);
    }

    /**
     * 查询药品详情
     *
     * <p>权限说明：登录后可访问
     */
    @GetMapping("/get")
    @Operation(summary = "查询药品详情", description = "根据ID查询药品详情")
    @AuthCheck
    public BaseResponse<DrugVO> getDrug(@Parameter(description = "药品ID") @RequestParam Long id) {
        DrugVO vo = drugService.getDrugById(id);
        return ResultUtils.success(vo);
    }

    /**
     * 根据编码查询药品
     *
     * <p>权限说明：登录后可访问
     */
    @GetMapping("/getByCode")
    @Operation(summary = "根据编码查询药品", description = "根据药品编码查询药品详情")
    @AuthCheck
    public BaseResponse<DrugVO> getDrugByCode(@Parameter(description = "药品编码") @RequestParam String drugCode) {
        DrugVO vo = drugService.getDrugByCode(drugCode);
        return ResultUtils.success(vo);
    }

    /**
     * 药品列表
     *
     * <p>权限说明：医生、药师、管理员可访问
     */
    @PostMapping("/list")
    @Operation(summary = "药品列表", description = "查询药品列表（带价格）")
    @AuthCheck(mustRoles = {UserConstant.DOCTOR_ROLE, UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<List<DrugWithPriceVO>> listDrugs(@RequestBody DrugQueryRequest request) {
        List<DrugWithPriceVO> list = drugService.listDrugs(request);
        return ResultUtils.success(list);
    }

    /**
     * 新增药品价格
     *
     * <p>权限说明：仅管理员可操作
     */
    @PostMapping("/price/add")
    @Operation(summary = "新增药品价格", description = "为药品添加新价格")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> addPrice(@RequestBody PriceAddRequest request) {
        drugService.addPrice(request);
        return ResultUtils.success( null);
    }

    /**
     * 获取药品当前价格
     *
     * <p>权限说明：登录后可访问
     */
    @GetMapping("/price/get")
    @Operation(summary = "获取药品当前价格", description = "获取药品当前有效价格")
    @AuthCheck
    public BaseResponse<BigDecimal> getCurrentPrice(
            @Parameter(description = "药品ID") @RequestParam Long drugId,
            @Parameter(description = "价格类型") @RequestParam(defaultValue = "RETAIL") String priceType) {
        BigDecimal price = drugService.getCurrentPrice(drugId, priceType);
        return ResultUtils.success(price);
    }

    /**
     * 根据编码获取价格
     *
     * <p>权限说明：登录后可访问
     */
    @GetMapping("/price/getByCode")
    @Operation(summary = "根据编码获取价格", description = "根据药品编码获取当前价格")
    @AuthCheck
    public BaseResponse<BigDecimal> getCurrentPriceByCode(
            @Parameter(description = "药品编码") @RequestParam String drugCode,
            @Parameter(description = "价格类型") @RequestParam(defaultValue = "RETAIL") String priceType) {
        BigDecimal price = drugService.getCurrentPriceByCode(drugCode, priceType);
        return ResultUtils.success(price);
    }

    /**
     * 计算费用
     *
     * <p>权限说明：医生、药师、管理员可访问
     */
    @PostMapping("/fee/calculate")
    @Operation(summary = "计算费用", description = "计算药品费用：总金额 = Σ(单价 × 数量)")
    @AuthCheck(mustRoles = {UserConstant.DOCTOR_ROLE, UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<BigDecimal> calculateFee(@RequestBody List<FeeCalculationRequest> items) {
        BigDecimal total = drugService.calculateFee(items);
        return ResultUtils.success(total);
    }

    /**
     * 获取药品类别
     *
     * <p>权限说明：登录后可访问
     */
    @GetMapping("/categories")
    @Operation(summary = "获取药品类别", description = "获取药品类别列表")
    @AuthCheck
    public BaseResponse<List<Map<String, String>>> getCategories() {
        List<Map<String, String>> categories = List.of(
                Map.of("code", "ANTIBIOTIC", "name", "抗生素"),
                Map.of("code", "COLD", "name", "感冒药"),
                Map.of("code", "ANALGESIC", "name", "解热镇痛药"),
                Map.of("code", "RESPIRATORY", "name", "呼吸系统药"),
                Map.of("code", "DIGESTIVE", "name", "消化系统药"),
                Map.of("code", "CARDIO", "name", "心血管药"),
                Map.of("code", "DIABETES", "name", "糖尿病药"),
                Map.of("code", "DERMATOLOGY", "name", "皮肤科药")
        );
        return ResultUtils.success(categories);
    }
}
