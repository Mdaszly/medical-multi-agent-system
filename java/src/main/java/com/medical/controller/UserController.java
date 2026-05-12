package com.medical.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.model.dto.user.UserLoginRequest;
import com.medical.model.dto.user.UserRegisterRequest;
import com.medical.model.vo.UserLoginVO;
import com.medical.model.vo.UserVO;
import com.medical.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户注册、登录、信息查询等接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册，需要账号、密码和确认密码，手机号和邮箱可选")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "40401", description = "账号已存在")
    })
    public BaseResponse<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("User register request: account={}", request.getUserAccount());
        Long userId = userService.register(request);
        return ResultUtils.success(userId, "注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用账号或手机号登录，登录成功返回token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "账号或密码错误"),
            @ApiResponse(responseCode = "40301", description = "账号已禁用")
    })
    public BaseResponse<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("User login request: account={}", request.getUserAccount());
        UserLoginVO loginVO = userService.login(request);
        return ResultUtils.success(loginVO, "登录成功");
    }

    @PostMapping("/logout")
    @Operation(summary = "用户退出", description = "清除当前用户的登录状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "退出成功"),
            @ApiResponse(responseCode = "40100", description = "用户未登录")
    })
    public BaseResponse<Void> logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("User logout request: id={}", userId);
        userService.logout();
        return ResultUtils.success(null, "退出成功");
    }

    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录用户", description = "获取当前登录用户的详细信息，需登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "40100", description = "用户未登录")
    })
    public BaseResponse<UserVO> getCurrentUser() {
        log.info("Get current user request");
        UserVO userVO = userService.getCurrentUser();
        return ResultUtils.success(userVO);
    }

    @PostMapping("/get/vo")
    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID获取用户详细信息，需登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "40100", description = "用户未登录"),
            @ApiResponse(responseCode = "40400", description = "用户不存在")
    })
    public BaseResponse<UserVO> getUserById(@RequestBody UserIdRequest request) {
        log.info("Get user by id request: id={}", request.getId());
        UserVO userVO = userService.getUserById(request.getId());
        return ResultUtils.success(userVO);
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
}
