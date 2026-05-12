package com.medical.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.annotation.AuthCheck;
import com.medical.common.BaseResponse;
import com.medical.common.ErrorCode;
import com.medical.common.ResultUtils;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.model.dto.user.UserQueryRequest;
import com.medical.model.dto.user.UserUpdateRequest;
import com.medical.model.vo.UserVO;
import com.medical.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户CRUD接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录用户", description = "获取当前登录用户的详细信息")
    public BaseResponse<UserVO> getCurrentUser() {
        log.info("Get current user request");
        UserVO userVO = userService.getCurrentUser();
        return ResultUtils.success(userVO);
    }

    @PostMapping("/get/vo")
    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID获取用户详细信息")
    public BaseResponse<UserVO> getUserById(@RequestBody UserIdRequest request) {
        log.info("Get user by id request: id={}", request.getId());
        UserVO userVO = userService.getUserById(request.getId());
        return ResultUtils.success(userVO);
    }

    @PostMapping("/list/page")
    @Operation(summary = "用户列表分页查询", description = "分页查询用户列表，支持条件筛选")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<UserVO>> listUserPage(@RequestBody UserQueryRequest request) {
        log.info("List user page request: current={}, pageSize={}", request.getCurrent(), request.getPageSize());
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("userAccount", request.getUserAccount());
        conditions.put("userName", request.getUserName());
        conditions.put("userRole", request.getUserRole());
        conditions.put("userStatus", request.getUserStatus());
        
        IPage<UserVO> page = userService.listUserPage(
                request.getCurrent() != null ? request.getCurrent() : 1,
                request.getPageSize() != null ? request.getPageSize() : 10,
                conditions);
        
        return ResultUtils.success(page);
    }

    @PostMapping("/update")
    @Operation(summary = "更新用户信息", description = "管理员更新用户信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserVO> updateUser(@RequestBody UserUpdateWithIdRequest request) {
        log.info("Update user request: id={}", request.getId());
        UserVO userVO = userService.updateUser(request.getId(), request);
        return ResultUtils.success(userVO, "更新成功");
    }

    @PostMapping("/update/profile")
    @Operation(summary = "更新个人资料", description = "用户更新自己的个人资料")
    public BaseResponse<UserVO> updateProfile(@RequestBody UserUpdateRequest request) {
        log.info("Update profile request");
        Long userId = getCurrentUserId();
        UserVO userVO = userService.updateProfile(userId, request);
        return ResultUtils.success(userVO, "更新成功");
    }

    @PostMapping("/change/password")
    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    public BaseResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        log.info("Change password request");
        Long userId = getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResultUtils.success(null, "密码修改成功");
    }

    @PostMapping("/bind/phone")
    @Operation(summary = "绑定手机号", description = "用户绑定手机号")
    public BaseResponse<Void> bindPhone(@RequestBody BindPhoneRequest request) {
        log.info("Bind phone request: phone={}", request.getPhone());
        Long userId = getCurrentUserId();
        userService.bindPhone(userId, request.getPhone());
        return ResultUtils.success(null, "手机号绑定成功");
    }

    @PostMapping("/bind/email")
    @Operation(summary = "绑定邮箱", description = "用户绑定邮箱")
    public BaseResponse<Void> bindEmail(@RequestBody BindEmailRequest request) {
        log.info("Bind email request: email={}", request.getEmail());
        Long userId = getCurrentUserId();
        userService.bindEmail(userId, request.getEmail());
        return ResultUtils.success(null, "邮箱绑定成功");
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户", description = "管理员删除用户")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> deleteUser(@RequestBody UserIdRequest request) {
        log.info("Delete user request: id={}", request.getId());
        userService.deleteUser(request.getId());
        return ResultUtils.success(null, "删除成功");
    }

    @PostMapping("/disable")
    @Operation(summary = "禁用用户", description = "管理员禁用用户账号")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> disableUser(@RequestBody UserIdRequest request) {
        log.info("Disable user request: id={}", request.getId());
        userService.disableUser(request.getId());
        return ResultUtils.success(null, "禁用成功");
    }

    @PostMapping("/enable")
    @Operation(summary = "启用用户", description = "管理员启用用户账号")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Void> enableUser(@RequestBody UserIdRequest request) {
        log.info("Enable user request: id={}", request.getId());
        userService.enableUser(request.getId());
        return ResultUtils.success(null, "启用成功");
    }

    private Long getCurrentUserId() {
        cn.dev33.satoken.stp.StpUtil.checkLogin();
        return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
    }

    public static class UserIdRequest {
        @Parameter(description = "用户ID", required = true)
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class UserUpdateWithIdRequest extends UserUpdateRequest {
        @Parameter(description = "用户ID", required = true)
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class ChangePasswordRequest {
        @Parameter(description = "原密码", required = true)
        private String oldPassword;

        @Parameter(description = "新密码", required = true)
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class BindPhoneRequest {
        @Parameter(description = "手机号", required = true)
        private String phone;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class BindEmailRequest {
        @Parameter(description = "邮箱", required = true)
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}