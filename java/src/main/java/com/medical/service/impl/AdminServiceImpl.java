package com.medical.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.ErrorCode;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.UserMapper;
import com.medical.model.dto.admin.AdminQueryRequest;
import com.medical.model.dto.admin.AdminUpdateRequest;
import com.medical.model.entity.User;
import com.medical.model.vo.AdminVO;
import com.medical.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;

    @Override
    public AdminVO getAdminById(Long id) {
        User admin = getAdminEntityById(id);
        return AdminVO.fromEntity(admin);
    }

    @Override
    public User getAdminEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "管理员ID无效");
        User admin = userMapper.selectById(id);
        ThrowUtils.throwIf(admin == null, ErrorCode.USER_NOT_FOUND);
        ThrowUtils.throwIf(!UserConstant.ADMIN_ROLE.equals(admin.getUserRole()), 
                ErrorCode.NO_PERMISSION, "非管理员账号");
        return admin;
    }

    @Override
    public AdminVO getCurrentAdmin() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long adminId = StpUtil.getLoginIdAsLong();
        return getAdminById(adminId);
    }

    @Override
    public IPage<AdminVO> listAdminPage(long current, long pageSize, AdminQueryRequest request) {
        log.info("List admin page: current={}, pageSize={}, request={}", current, pageSize, request);
        
        Page<User> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(User::getUserRole, UserConstant.ADMIN_ROLE);
        
        if (request != null) {
            if (StringUtils.hasText(request.getUserAccount())) {
                wrapper.like(User::getUserAccount, request.getUserAccount());
            }
            if (StringUtils.hasText(request.getUserName())) {
                wrapper.like(User::getUserName, request.getUserName());
            }
            if (request.getUserStatus() != null) {
                wrapper.eq(User::getUserStatus, request.getUserStatus());
            }
        }
        
        wrapper.eq(User::getIsDelete, UserConstant.NOT_DELETED)
               .orderByDesc(User::getCreateTime);
        
        IPage<User> adminPage = userMapper.selectPage(page, wrapper);
        
        return adminPage.convert(AdminVO::fromEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminVO addAdmin(AdminUpdateRequest request) {
        log.info("Add admin: account={}", request.getUserAccount());
        
        ThrowUtils.throwIf(!StringUtils.hasText(request.getUserAccount()), 
                ErrorCode.PARAM_ERROR, "账号不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(request.getUserName()), 
                ErrorCode.PARAM_ERROR, "姓名不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(request.getPassword()), 
                ErrorCode.PARAM_ERROR, "密码不能为空");
        
        checkAccountExists(request.getUserAccount());
        
        validatePassword(request.getPassword());
        
        String salt = generateSalt();
        String encryptedPassword = encryptPassword(request.getPassword(), salt);
        
        User admin = new User();
        admin.setUserAccount(request.getUserAccount());
        admin.setUserPassword(encryptedPassword);
        admin.setSalt(salt);
        admin.setUserName(request.getUserName());
        admin.setUserRole(UserConstant.ADMIN_ROLE);
        
        if (request.getPhone() != null) {
            validatePhone(request.getPhone());
            admin.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
            admin.setEmail(request.getEmail());
        }
        
        admin.setUserStatus(UserConstant.STATUS_NORMAL);
        
        int result = userMapper.insert(admin);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "添加失败");
        
        log.info("Admin added successfully: id={}, account={}", admin.getId(), request.getUserAccount());
        return AdminVO.fromEntity(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminVO updateAdmin(Long id, AdminUpdateRequest request) {
        log.info("Update admin: id={}, request={}", id, request);
        
        User admin = getAdminEntityById(id);
        
        if (request.getUserName() != null) {
            admin.setUserName(request.getUserName());
        }
        if (request.getPhone() != null) {
            validatePhone(request.getPhone());
            checkPhoneExists(request.getPhone(), id);
            admin.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
            checkEmailExists(request.getEmail(), id);
            admin.setEmail(request.getEmail());
        }
        if (request.getUserStatus() != null) {
            admin.setUserStatus(request.getUserStatus());
        }
        
        admin.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(admin);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "更新失败");
        
        log.info("Admin updated successfully: id={}", id);
        return AdminVO.fromEntity(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdmin(Long id) {
        log.info("Delete admin: id={}", id);
        
        User admin = getAdminEntityById(id);
        
        admin.setIsDelete(UserConstant.IS_DELETED);
        admin.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(admin);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "删除失败");
        
        log.info("Admin deleted successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        log.info("Reset admin password: id={}", id);
        
        ThrowUtils.throwIf(!StringUtils.hasText(newPassword), ErrorCode.PARAM_ERROR, "密码不能为空");
        
        validatePassword(newPassword);
        
        User admin = getAdminEntityById(id);
        
        String newSalt = generateSalt();
        String encryptedPassword = encryptPassword(newPassword, newSalt);
        
        admin.setUserPassword(encryptedPassword);
        admin.setSalt(newSalt);
        admin.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(admin);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "重置密码失败");
        
        log.info("Admin password reset successfully: id={}", id);
    }

    private void validatePhone(String phone) {
        ThrowUtils.throwIf(!phone.matches("^1[3-9]\\d{9}$"), ErrorCode.PARAM_ERROR, "手机号格式不正确");
    }

    private void validateEmail(String email) {
        ThrowUtils.throwIf(!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"), 
                ErrorCode.PARAM_ERROR, "邮箱格式不正确");
    }

    private void validatePassword(String password) {
        ThrowUtils.throwIf(password.length() < 8 || password.length() > 20, 
                ErrorCode.PARAM_ERROR, "密码长度必须在8-20位之间");
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        ThrowUtils.throwIf(!(hasUpper && hasLower && hasDigit), 
                ErrorCode.PARAM_ERROR, "密码必须包含大小写字母和数字");
    }

    private void checkAccountExists(String account) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserAccount, account)
               .eq(User::getIsDelete, UserConstant.NOT_DELETED);
        if (userMapper.exists(wrapper)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该账号已存在");
        }
    }

    private void checkPhoneExists(String phone, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone)
               .ne(excludeId != null, User::getId, excludeId)
               .eq(User::getIsDelete, UserConstant.NOT_DELETED);
        if (userMapper.exists(wrapper)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该手机号已被使用");
        }
    }

    private void checkEmailExists(String email, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email)
               .ne(excludeId != null, User::getId, excludeId)
               .eq(User::getIsDelete, UserConstant.NOT_DELETED);
        if (userMapper.exists(wrapper)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该邮箱已被使用");
        }
    }

    private String generateSalt() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String encryptPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String input = salt + password;
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码加密失败");
        }
    }
}