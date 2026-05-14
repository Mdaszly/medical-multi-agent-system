package com.medical.controller;

import com.medical.common.BaseResponse;
import com.medical.common.ResultUtils;
import com.medical.model.dto.admin.AdminUpdateRequest;
import com.medical.model.vo.AdminVO;
import com.medical.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * ========================================
 * TEMPORARY DEVELOPMENT CONTROLLER - TO BE DELETED
 * ========================================
 *
 * WARNING: THIS CONTROLLER IS FOR DEVELOPMENT AND TESTING PURPOSES ONLY.
 * DO NOT USE IN PRODUCTION ENVIRONMENT!
 *
 * TASK: Temporary admin registration for development testing
 * CREATED: 2026-05-13
 * DELETE BY: 2026-06-13
 *
 * DELETE TAGS:
 * - [TEMP-20260513] - Temporary admin controller
 * ========================================
 */
@Slf4j
@RestController
@RequestMapping("/api/temp/admin")
@RequiredArgsConstructor
@Tag(name = "【临时】管理员管理", description = "仅用于开发测试 - 2026-06-13前删除")
public class TempAdminController {

    private final AdminService adminService;

    /**
     * ========================================
     * TEMPORARY ENDPOINT - TO BE DELETED
     * ========================================
     *
     * WARNING: NO AUTHENTICATION REQUIRED!
     * FOR DEVELOPMENT TESTING ONLY.
     *
     * TASK: Register admin without authentication
     * DELETE BY: 2026-06-13
     * DELETE TAG: [TEMP-20260513-01]
     * ========================================
     */
    @PostMapping("/register")
    @Operation(summary = "【临时】注册管理员", description = "开发测试用 - 无需认证 - 2026-06-13前删除")
    public BaseResponse<AdminVO> tempRegisterAdmin(@RequestBody AdminUpdateRequest request) {
        log.warn("TEMPORARY ENDPOINT CALLED - /api/temp/admin/register");
        log.warn("This endpoint should be deleted by 2026-06-13");
        
        AdminVO adminVO = adminService.addAdmin(request);
        return ResultUtils.success(adminVO, "临时注册成功");
    }
}
