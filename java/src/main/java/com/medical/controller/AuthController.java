package com.medical.controller;

import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.model.dto.AuthLoginRequest;
import com.medical.model.dto.AuthRegisterRequest;
import com.medical.model.vo.AuthLoginVO;
import com.medical.model.vo.AuthRegisterVO;
import com.medical.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "统一注册", description = "患者/医生统一注册接口，用户角色通过userRole字段指定")
    public BaseResponse<AuthRegisterVO> register(@Valid @RequestBody AuthRegisterRequest request) {
        log.info("Auth register request: account={}, role={}", request.getUserAccount(), request.getUserRole());
        AuthRegisterVO registerVO = authService.register(request);
        return ResultUtils.success(registerVO, "注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "统一登录", description = "患者/医生/管理员统一登录接口，使用账号密码登录")
    public BaseResponse<AuthLoginVO> login(@Valid @RequestBody AuthLoginRequest request) {
        log.info("Auth login request: account={}", request.getUserAccount());
        AuthLoginVO loginVO = authService.login(request);
        return ResultUtils.success(loginVO, "登录成功");
    }

    @PostMapping("/logout")
    @Operation(summary = "统一退出", description = "清除当前用户的登录状态")
    public BaseResponse<Void> logout() {
        authService.logout();
        return ResultUtils.success(null, "退出成功");
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的详细信息")
    public BaseResponse<AuthLoginVO> getCurrentUser() {
        AuthLoginVO loginVO = authService.getCurrentUser();
        return ResultUtils.success(loginVO);
    }
}