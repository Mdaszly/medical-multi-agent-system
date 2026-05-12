package com.medical.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.doctor.DoctorQueryRequest;
import com.medical.model.dto.doctor.DoctorUpdateRequest;
import com.medical.model.vo.DoctorVO;
import com.medical.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Tag(name = "医生管理", description = "医生CRUD接口")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录医生", description = "获取当前登录医生的详细信息")
    public BaseResponse<DoctorVO> getCurrentDoctor() {
        log.info("Get current doctor request");
        DoctorVO doctorVO = doctorService.getCurrentDoctor();
        return ResultUtils.success(doctorVO);
    }

    @PostMapping("/get")
    @Operation(summary = "根据ID获取医生信息", description = "根据医生ID获取医生详细信息")
    public BaseResponse<DoctorVO> getDoctorById(@RequestBody DoctorIdRequest request) {
        log.info("Get doctor by id request: id={}", request.getId());
        DoctorVO doctorVO = doctorService.getDoctorById(request.getId());
        return ResultUtils.success(doctorVO);
    }

    @PostMapping("/list")
    @Operation(summary = "医生列表查询", description = "分页查询医生列表，支持条件筛选")
    public BaseResponse<IPage<DoctorVO>> listDoctorPage(@RequestBody DoctorQueryRequest request) {
        log.info("List doctor page request: current={}, pageSize={}", request.getCurrent(), request.getPageSize());
        
        IPage<DoctorVO> page = doctorService.listDoctorPage(
                request.getCurrent() != null ? request.getCurrent() : 1,
                request.getPageSize() != null ? request.getPageSize() : 10,
                request);
        
        return ResultUtils.success(page);
    }

    @GetMapping("/list/department")
    @Operation(summary = "按科室查询医生", description = "查询指定科室的在岗医生列表")
    public BaseResponse<List<DoctorVO>> listDoctorByDepartment(
            @RequestParam("department") String department) {
        log.info("List doctor by department request: {}", department);
        List<DoctorVO> doctors = doctorService.listDoctorByDepartment(department);
        return ResultUtils.success(doctors);
    }

    @PostMapping("/add")
    @Operation(summary = "新增医生", description = "管理员新增医生")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DoctorVO> addDoctor(@RequestBody DoctorUpdateRequest request) {
        log.info("Add doctor request: name={}", request.getDoctorName());
        DoctorVO doctorVO = doctorService.addDoctor(request);
        return ResultUtils.success(doctorVO, "添加成功");
    }

    @PostMapping("/update")
    @Operation(summary = "更新医生信息", description = "管理员更新医生信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DoctorVO> updateDoctor(@RequestBody DoctorUpdateWithIdRequest request) {
        log.info("Update doctor request: id={}", request.getId());
        DoctorVO doctorVO = doctorService.updateDoctor(request.getId(), request);
        return ResultUtils.success(doctorVO, "更新成功");
    }

    @PostMapping("/update/profile")
    @Operation(summary = "更新个人资料", description = "医生更新自己的个人资料")
    @AuthCheck(mustRole = UserConstant.DOCTOR_ROLE)
    public BaseResponse<DoctorVO> updateProfile(@RequestBody DoctorUpdateRequest request) {
        log.info("Update doctor profile request");
        Long doctorId = getCurrentDoctorId();
        DoctorVO doctorVO = doctorService.updateProfile(doctorId, request);
        return ResultUtils.success(doctorVO, "更新成功");
    }

    @PostMapping("/delete")
    @Operation(summary = "删除医生", description = "管理员删除医生")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> deleteDoctor(@RequestBody DoctorIdRequest request) {
        log.info("Delete doctor request: id={}", request.getId());
        doctorService.deleteDoctor(request.getId());
        return ResultUtils.success(null, "删除成功");
    }

    @PostMapping("/disable")
    @Operation(summary = "禁用医生", description = "管理员禁用医生账号")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> disableDoctor(@RequestBody DoctorIdRequest request) {
        log.info("Disable doctor request: id={}", request.getId());
        doctorService.disableDoctor(request.getId());
        return ResultUtils.success(null, "禁用成功");
    }

    @PostMapping("/enable")
    @Operation(summary = "启用医生", description = "管理员启用医生账号")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> enableDoctor(@RequestBody DoctorIdRequest request) {
        log.info("Enable doctor request: id={}", request.getId());
        doctorService.enableDoctor(request.getId());
        return ResultUtils.success(null, "启用成功");
    }

    private Long getCurrentDoctorId() {
        cn.dev33.satoken.stp.StpUtil.checkLogin();
        return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
    }

    public static class DoctorIdRequest {
        @Parameter(description = "医生ID", required = true)
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class DoctorUpdateWithIdRequest extends DoctorUpdateRequest {
        @Parameter(description = "医生ID", required = true)
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
