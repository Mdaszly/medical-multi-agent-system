package com.medical.controller;

import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ErrorCode;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.mapper.HealthProfileMapper;
import com.medical.model.entity.HealthProfile;
import com.medical.model.entity.User;
import com.medical.model.vo.HealthProfileVO;
import com.medical.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/health-profile")
@RequiredArgsConstructor
@Tag(name = "健康档案管理", description = "健康档案相关接口")
public class HealthProfileController {

    private final HealthProfileMapper healthProfileMapper;
    private final UserMapper userMapper;

    @GetMapping("/get")
    @Operation(summary = "获取健康档案", description = "根据用户ID获取健康档案")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.PHARMACIST_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<HealthProfileVO> getHealthProfile(
            @Parameter(description = "用户ID")
            @RequestParam(value = "userId", required = false) Long userId) {
        
        Long currentUserId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        String currentRole = (String) cn.dev33.satoken.stp.StpUtil.getSession().get("userRole");
        
        // 确定要查询的用户ID
        Long targetUserId = userId != null ? userId : currentUserId;
        
        // 权限校验
        if (!UserConstant.ADMIN_ROLE.equals(currentRole)) {
            // 用户只能查看自己的健康档案
            if (UserConstant.USER_ROLE.equals(currentRole) && !currentUserId.equals(targetUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权查看他人健康档案");
            }
            // 医生和药师需要验证是否有业务关联
        }
        
        HealthProfile profile = healthProfileMapper.selectByUserId(targetUserId);
        
        if (profile == null) {
            return ResultUtils.success(null);
        }
        
        return ResultUtils.success(HealthProfileVO.fromEntity(profile));
    }

    @PostMapping("/update")
    @Operation(summary = "更新健康档案", description = "更新用户健康档案信息")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.DOCTOR_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<HealthProfileVO> updateHealthProfile(
            @RequestBody HealthProfile request) {
        
        Long currentUserId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        String currentRole = (String) cn.dev33.satoken.stp.StpUtil.getSession().get("userRole");
        
        // 权限校验
        if (!UserConstant.ADMIN_ROLE.equals(currentRole) && 
            !currentUserId.equals(request.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权修改他人健康档案");
        }
        
        HealthProfile existing = healthProfileMapper.selectByUserId(request.getUserId());
        
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "健康档案不存在");
        }
        
        existing.setChronicDiseases(request.getChronicDiseases());
        existing.setAllergyHistory(request.getAllergyHistory());
        existing.setMedicationHistory(request.getMedicationHistory());
        existing.setFamilyHistory(request.getFamilyHistory());
        existing.setSurgicalHistory(request.getSurgicalHistory());
        existing.setVaccinationHistory(request.getVaccinationHistory());
        existing.setPhysicalExam(request.getPhysicalExam());
        existing.setHeight(request.getHeight());
        existing.setWeight(request.getWeight());
        existing.setBloodType(request.getBloodType());
        existing.setBloodPressure(request.getBloodPressure());
        existing.setRemark(request.getRemark());
        
        healthProfileMapper.updateById(existing);
        
        return ResultUtils.success(HealthProfileVO.fromEntity(existing));
    }

    @PostMapping("/create")
    @Operation(summary = "创建健康档案", description = "为用户创建健康档案")
    @AuthCheck(mustRoles = {UserConstant.USER_ROLE, UserConstant.ADMIN_ROLE})
    public BaseResponse<HealthProfileVO> createHealthProfile(
            @RequestBody HealthProfile request) {
        
        Long currentUserId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        String currentRole = (String) cn.dev33.satoken.stp.StpUtil.getSession().get("userRole");
        
        // 用户只能为自己创建健康档案
        if (!UserConstant.ADMIN_ROLE.equals(currentRole) && 
            !currentUserId.equals(request.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权为他人创建健康档案");
        }
        
        // 检查是否已存在
        HealthProfile existing = healthProfileMapper.selectByUserId(request.getUserId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "健康档案已存在");
        }
        
        User user = userMapper.selectById(request.getUserId());
        if (user != null) {
            request.setUserName(user.getUserName());
        }
        
        healthProfileMapper.insert(request);
        
        return ResultUtils.success(HealthProfileVO.fromEntity(request));
    }
}