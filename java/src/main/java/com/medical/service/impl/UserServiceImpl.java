package com.medical.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.ErrorCode;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.UserMapper;
import com.medical.model.dto.user.UserLoginRequest;
import com.medical.model.dto.user.UserRegisterRequest;
import com.medical.model.entity.User;
import com.medical.model.vo.UserLoginVO;
import com.medical.model.vo.UserVO;
import com.medical.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional
    public Long register(UserRegisterRequest request) {
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();

        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PASSWORD_NOT_MATCH);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserAccount, userAccount);
        ThrowUtils.throwIf(userMapper.selectCount(wrapper) > 0, ErrorCode.USER_ALREADY_EXISTS, "账号已存在");

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(User::getPhone, request.getPhone());
            ThrowUtils.throwIf(userMapper.selectCount(phoneWrapper) > 0, ErrorCode.USER_ALREADY_EXISTS, "手机号已被注册");
        }

        String salt = RandomStringUtils.randomAlphanumeric(20);
        String encryptedPassword = encryptPassword(userPassword, salt);

        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setSalt(salt);
        user.setUserName(request.getUserName() != null ? request.getUserName() : generateNickname());
        user.setUserRole(UserConstant.USER_ROLE);
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        user.setUserStatus(UserConstant.STATUS_NORMAL);
        user.setIsDelete(UserConstant.NOT_DELETED);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("User registered successfully: id={}, account={}", user.getId(), userAccount);
        return user.getId();
    }

    @Override
    @Transactional
    public UserLoginVO login(UserLoginRequest request) {
        String password = request.getPassword();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (request.getUserAccount() != null && !request.getUserAccount().isEmpty()) {
            wrapper.eq(User::getUserAccount, request.getUserAccount());
        } else if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            wrapper.eq(User::getPhone, request.getPhone());
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号或手机号不能为空");
        }

        User user = userMapper.selectOne(wrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND);

        ThrowUtils.throwIf(user.getUserStatus().equals(UserConstant.STATUS_DISABLED),
                ErrorCode.ACCOUNT_DISABLED);

        String encryptedPassword = encryptPassword(password, user.getSalt());
        ThrowUtils.throwIf(!encryptedPassword.equals(user.getUserPassword()),
                ErrorCode.PARAM_ERROR, "密码错误");

        userMapper.updateById(user);

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setId(user.getId());
        loginVO.setToken(token);
        loginVO.setUserName(user.getUserName());
        loginVO.setUserRole(user.getUserRole());
        loginVO.setExpireTime(LocalDateTime.now().plusDays(7));

        log.info("User logged in successfully: id={}, account={}", user.getId(), user.getUserAccount());
        return loginVO;
    }

    @Override
    public void logout() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("User logged out: id={}", userId);
    }

    @Override
    public UserVO getCurrentUser() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        User user = getUserEntityById(userId);
        return UserVO.fromEntity(user);
    }

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

    private String generateNickname() {
        return "用户" + System.currentTimeMillis() % 10000;
    }
}
