package com.medical.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.model.dto.admin.AdminQueryRequest;
import com.medical.model.dto.admin.AdminUpdateRequest;
import com.medical.model.vo.AdminVO;
import com.medical.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员CRUD接口")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录管理员", description = "获取当前登录管理员的详细信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminVO> getCurrentAdmin() {
        log.info("Get current admin request");
        AdminVO adminVO = adminService.getCurrentAdmin();
        return ResultUtils.success(adminVO);
    }

    @PostMapping("/get")
    @Operation(summary = "根据ID获取管理员信息", description = "根据管理员ID获取管理员详细信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminVO> getAdminById(@RequestBody AdminIdRequest request) {
        log.info("Get admin by id request: id={}", request.getId());
        AdminVO adminVO = adminService.getAdminById(request.getId());
        return ResultUtils.success(adminVO);
    }

    @PostMapping("/list/page")
    @Operation(summary = "管理员列表分页查询", description = "分页查询管理员列表，支持条件筛选")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<AdminVO>> listAdminPage(@RequestBody AdminQueryRequest request) {
        log.info("List admin page request: current={}, pageSize={}", request.getCurrent(), request.getPageSize());
        
        IPage<AdminVO> page = adminService.listAdminPage(
                request.getCurrent() != null ? request.getCurrent() : 1,
                request.getPageSize() != null ? request.getPageSize() : 10,
                request);
        
        return ResultUtils.success(page);
    }

    @PostMapping("/add")
    @Operation(summary = "新增管理员", description = "超级管理员新增管理员")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminVO> addAdmin(@RequestBody AdminUpdateRequest request) {
        log.info("Add admin request: account={}", request.getUserAccount());
        AdminVO adminVO = adminService.addAdmin(request);
        return ResultUtils.success(adminVO, "添加成功");
    }

    @PostMapping("/update")
    @Operation(summary = "更新管理员信息", description = "超级管理员更新管理员信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminVO> updateAdmin(@RequestBody AdminUpdateWithIdRequest request) {
        log.info("Update admin request: id={}", request.getId());
        AdminVO adminVO = adminService.updateAdmin(request.getId(), request);
        return ResultUtils.success(adminVO, "更新成功");
    }

    @PostMapping("/delete")
    @Operation(summary = "删除管理员", description = "超级管理员删除管理员")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> deleteAdmin(@RequestBody AdminIdRequest request) {
        log.info("Delete admin request: id={}", request.getId());
        adminService.deleteAdmin(request.getId());
        return ResultUtils.success(null, "删除成功");
    }

    @PostMapping("/reset/password")
    @Operation(summary = "重置密码", description = "超级管理员重置管理员密码")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("Reset admin password request: id={}", request.getId());
        adminService.resetPassword(request.getId(), request.getNewPassword());
        return ResultUtils.success(null, "密码重置成功");
    }

    public static class AdminIdRequest {
        @Parameter(description = "管理员ID", required = true)
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class AdminUpdateWithIdRequest extends AdminUpdateRequest {
        @Parameter(description = "管理员ID", required = true)
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class ResetPasswordRequest {
        @Parameter(description = "管理员ID", required = true)
        private Long id;

        @Parameter(description = "新密码", required = true)
        private String newPassword;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}