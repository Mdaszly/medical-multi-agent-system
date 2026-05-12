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
import com.medical.model.dto.user.UserUpdateRequest;
import com.medical.model.entity.User;
import com.medical.model.vo.UserVO;
import com.medical.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO getUserById(Long id) {
        User user = getUserEntityById(id);
        return UserVO.fromEntity(user);
    }

    @Override
    public User getUserEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        User user = userMapper.selectById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
        return user;
    }

    @Override
    public UserVO getCurrentUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return getUserById(userId);
    }

    @Override
    public IPage<UserVO> listUserPage(long current, long pageSize, Map<String, Object> conditions) {
        log.info("List user page: current={}, pageSize={}, conditions={}", current, pageSize, conditions);
        
        Page<User> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (conditions != null) {
            if (conditions.containsKey("userAccount")) {
                String account = (String) conditions.get("userAccount");
                if (StringUtils.hasText(account)) {
                    wrapper.like(User::getUserAccount, account);
                }
            }
            if (conditions.containsKey("userName")) {
                String name = (String) conditions.get("userName");
                if (StringUtils.hasText(name)) {
                    wrapper.like(User::getUserName, name);
                }
            }
            if (conditions.containsKey("userRole")) {
                String role = (String) conditions.get("userRole");
                if (StringUtils.hasText(role)) {
                    wrapper.eq(User::getUserRole, role);
                }
            }
            if (conditions.containsKey("userStatus")) {
                Integer status = (Integer) conditions.get("userStatus");
                if (status != null) {
                    wrapper.eq(User::getUserStatus, status);
                }
            }
        }
        
        wrapper.eq(User::getIsDelete, UserConstant.NOT_DELETED)
               .orderByDesc(User::getCreateTime);
        
        IPage<User> userPage = userMapper.selectPage(page, wrapper);
        
        return userPage.convert(UserVO::fromEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        log.info("Update user: id={}, request={}", id, request);
        
        User user = getUserEntityById(id);
        
        if (request.getUserName() != null) {
            user.setUserName(request.getUserName());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            validatePhone(request.getPhone());
            checkPhoneExists(request.getPhone(), id);
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
            checkEmailExists(request.getEmail(), id);
            user.setEmail(request.getEmail());
        }
        if (request.getUserStatus() != null) {
            user.setUserStatus(request.getUserStatus());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "更新失败");
        
        log.info("User updated successfully: id={}", id);
        return UserVO.fromEntity(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long id, UserUpdateRequest request) {
        log.info("Update profile: id={}", id);
        
        User user = getUserEntityById(id);
        
        if (request.getUserName() != null) {
            user.setUserName(request.getUserName());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "更新失败");
        
        log.info("Profile updated successfully: id={}", id);
        return UserVO.fromEntity(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String oldPassword, String newPassword) {
        log.info("Change password: id={}", id);
        
        ThrowUtils.throwIf(!StringUtils.hasText(oldPassword), ErrorCode.PARAM_ERROR, "原密码不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(newPassword), ErrorCode.PARAM_ERROR, "新密码不能为空");
        
        validatePassword(newPassword);
        
        User user = getUserEntityById(id);
        
        String encryptedOldPassword = encryptPassword(oldPassword, user.getSalt());
        if (!encryptedOldPassword.equals(user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码错误");
        }
        
        String newSalt = generateSalt();
        String encryptedNewPassword = encryptPassword(newPassword, newSalt);
        
        user.setUserPassword(encryptedNewPassword);
        user.setSalt(newSalt);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "密码修改失败");
        
        log.info("Password changed successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(Long id, String phone) {
        log.info("Bind phone: id={}, phone={}", id, phone);
        
        validatePhone(phone);
        checkPhoneExists(phone, id);
        
        User user = getUserEntityById(id);
        user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "绑定失败");
        
        log.info("Phone bound successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindEmail(Long id, String email) {
        log.info("Bind email: id={}, email={}", id, email);
        
        validateEmail(email);
        checkEmailExists(email, id);
        
        User user = getUserEntityById(id);
        user.setEmail(email);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "绑定失败");
        
        log.info("Email bound successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        log.info("Delete user: id={}", id);
        
        User user = getUserEntityById(id);
        
        if (UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员账号不能删除");
        }
        
        user.setIsDelete(UserConstant.IS_DELETED);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "删除失败");
        
        log.info("User deleted successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long id) {
        log.info("Disable user: id={}", id);
        
        User user = getUserEntityById(id);
        
        if (UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "不能禁用管理员账号");
        }
        
        user.setUserStatus(UserConstant.STATUS_DISABLED);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "禁用失败");
        
        log.info("User disabled successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long id) {
        log.info("Enable user: id={}", id);
        
        User user = getUserEntityById(id);
        
        user.setUserStatus(UserConstant.STATUS_NORMAL);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.updateById(user);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "启用失败");
        
        log.info("User enabled successfully: id={}", id);
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