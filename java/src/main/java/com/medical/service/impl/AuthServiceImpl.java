package com.medical.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.ErrorCode;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.UserMapper;
import com.medical.model.dto.AuthLoginRequest;
import com.medical.model.dto.AuthRegisterRequest;
import com.medical.model.entity.Doctor;
import com.medical.model.entity.User;
import com.medical.model.vo.AuthLoginVO;
import com.medical.model.vo.AuthRegisterVO;
import com.medical.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;

    @Override
    @Transactional
    public AuthRegisterVO register(AuthRegisterRequest request) {
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();
        String userRole = request.getUserRole();

        if (userAccount == null || userAccount.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能为空");
        }
        if (userPassword == null || userPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        if (checkPassword == null || checkPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "确认密码不能为空");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        if (userRole == null || userRole.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色不能为空");
        }
        if (!UserConstant.USER_ROLE.equals(userRole) && !UserConstant.DOCTOR_ROLE.equals(userRole) && !UserConstant.PHARMACIST_ROLE.equals(userRole)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的角色类型");
        }

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserAccount, userAccount);
        User existingUser = userMapper.selectOne(userWrapper);
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }

        LambdaQueryWrapper<Doctor> doctorWrapper = new LambdaQueryWrapper<>();
        doctorWrapper.eq(Doctor::getUserAccount, userAccount);
        Doctor existingDoctor = doctorMapper.selectOne(doctorWrapper);
        if (existingDoctor != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }

        if (UserConstant.DOCTOR_ROLE.equals(userRole)) {
            if (request.getLicenseNo() == null || request.getLicenseNo().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "医生执业证书编号不能为空");
            }
            LambdaQueryWrapper<Doctor> licenseWrapper = new LambdaQueryWrapper<>();
            licenseWrapper.eq(Doctor::getLicenseNo, request.getLicenseNo());
            Doctor doctorByLicense = doctorMapper.selectOne(licenseWrapper);
            if (doctorByLicense != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "执业证书编号已存在");
            }
            if (request.getDepartment() == null || request.getDepartment().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "科室不能为空");
            }
            if (request.getTitle() == null || request.getTitle().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "职称不能为空");
            }
        }

        String salt = generateSalt();
        String encryptedPassword = encryptPassword(userPassword, salt);

        if (UserConstant.USER_ROLE.equals(userRole) || UserConstant.PHARMACIST_ROLE.equals(userRole)) {
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptedPassword);
            user.setSalt(salt);
            user.setUserName(request.getUserName());
            user.setUserRole(userRole);
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setUserStatus(UserConstant.STATUS_NORMAL);

            int result = userMapper.insert(user);
            if (result == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
            }

            log.info("User registered successfully: id={}, account={}, role={}", 
                    user.getId(), userAccount, userRole);
            return buildRegisterVOForUser(user);
        } else {
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptedPassword);
            user.setSalt(salt);
            user.setUserName(request.getUserName());
            user.setUserRole(userRole);
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setUserStatus(UserConstant.STATUS_NORMAL);

            int userResult = userMapper.insert(user);
            if (userResult == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
            }

            Doctor doctor = new Doctor();
            doctor.setUserId(user.getId());
            doctor.setDoctorNo("DOC" + System.currentTimeMillis());
            doctor.setDoctorName(request.getUserName());
            doctor.setDepartment(request.getDepartment());
            doctor.setTitle(request.getTitle());
            doctor.setLicenseNo(request.getLicenseNo());
            doctor.setConsultationFee(request.getConsultationFee());
            doctor.setDescription(request.getUserName());
            doctor.setPhone(request.getPhone());
            doctor.setEmail(request.getEmail());
            doctor.setWorkStatus(UserConstant.DOCTOR_STATUS_ONLINE);
            doctor.setUserAccount(userAccount);
            doctor.setUserPassword(encryptedPassword);
            doctor.setSalt(salt);

            int doctorResult = doctorMapper.insert(doctor);
            if (doctorResult == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
            }

            log.info("Doctor registered successfully: userId={}, doctorId={}, account={}, name={}", 
                    user.getId(), doctor.getId(), userAccount, request.getUserName());
            return buildRegisterVOForDoctor(doctor);
        }
    }

    private AuthRegisterVO buildRegisterVOForUser(User user) {
        AuthRegisterVO vo = new AuthRegisterVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserRole(user.getUserRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private AuthRegisterVO buildRegisterVOForDoctor(Doctor doctor) {
        AuthRegisterVO vo = new AuthRegisterVO();
        vo.setId(doctor.getId());
        vo.setUserAccount(doctor.getUserAccount());
        vo.setUserName(doctor.getDoctorName());
        vo.setUserRole(UserConstant.DOCTOR_ROLE);
        vo.setCreateTime(doctor.getCreateTime());
        return vo;
    }

    private String generateSalt() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Override
    @Transactional
    public AuthRegisterVO registerAdmin(AuthRegisterRequest request) {
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();
        String userName = request.getUserName();

        if (userAccount == null || userAccount.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能为空");
        }
        if (userPassword == null || userPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        if (checkPassword == null || checkPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "确认密码不能为空");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        if (userName == null || userName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "姓名不能为空");
        }

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserAccount, userAccount);
        User existingUser = userMapper.selectOne(userWrapper);
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }

        String salt = generateSalt();
        String encryptedPassword = encryptPassword(userPassword, salt);

        User admin = new User();
        admin.setUserAccount(userAccount);
        admin.setUserPassword(encryptedPassword);
        admin.setSalt(salt);
        admin.setUserName(userName);
        admin.setUserRole(UserConstant.ADMIN_ROLE);
        admin.setAdminAccountType(UserConstant.ADMIN_TYPE_TEMPORARY);
        admin.setUserStatus(UserConstant.STATUS_NORMAL);
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            admin.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            admin.setEmail(request.getEmail());
        }

        int result = userMapper.insert(admin);
        if (result == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }

        log.info("Temporary admin registered: id={}, account={}, type=TEMPORARY",
                admin.getId(), userAccount);

        AuthRegisterVO vo = new AuthRegisterVO();
        vo.setId(admin.getId());
        vo.setUserAccount(admin.getUserAccount());
        vo.setUserName(admin.getUserName());
        vo.setUserRole(UserConstant.ADMIN_ROLE);
        vo.setCreateTime(admin.getCreateTime());
        return vo;
    }

    @Override
    @Transactional
    public AuthLoginVO login(AuthLoginRequest request) {
        String userAccount = request.getUserAccount();
        String password = request.getPassword();

        if (userAccount == null || userAccount.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserAccount, userAccount);
        User user = userMapper.selectOne(userWrapper);

        if (user != null) {
            if (user.getUserStatus() == null || user.getUserStatus().equals(UserConstant.STATUS_DISABLED)) {
                throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
            }

            String encryptedPassword = encryptPassword(password, user.getSalt());
            if (!encryptedPassword.equals(user.getUserPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误");
            }

            StpUtil.login(user.getId());
            String token = StpUtil.getTokenValue();

            log.info("User logged in successfully: id={}, account={}, role={}", 
                    user.getId(), userAccount, user.getUserRole());
            return buildLoginVOFromUser(user, token);
        }

        LambdaQueryWrapper<Doctor> doctorWrapper = new LambdaQueryWrapper<>();
        doctorWrapper.eq(Doctor::getUserAccount, userAccount);
        Doctor doctor = doctorMapper.selectOne(doctorWrapper);

        if (doctor == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (doctor.getWorkStatus() == null || doctor.getWorkStatus() != UserConstant.DOCTOR_STATUS_ONLINE) {
            throw new BusinessException(ErrorCode.NO_AUTH, "医生当前不在岗");
        }

        String encryptedPassword = encryptPassword(password, doctor.getSalt());
        if (!encryptedPassword.equals(doctor.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误");
        }

        StpUtil.login(doctor.getId());
        String token = StpUtil.getTokenValue();

        log.info("Doctor logged in successfully: id={}, account={}, name={}", 
                doctor.getId(), userAccount, doctor.getDoctorName());
        return buildLoginVOFromDoctor(doctor, token);
    }

    @Override
    public void logout() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("User logged out: id={}", userId);
    }

    @Override
    public AuthLoginVO getCurrentUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        
        User user = userMapper.selectById(userId);
        if (user != null) {
            String token = StpUtil.getTokenValue();
            return buildLoginVOFromUser(user, token);
        }

        Doctor doctor = doctorMapper.selectById(userId);
        if (doctor != null) {
            String token = StpUtil.getTokenValue();
            return buildLoginVOFromDoctor(doctor, token);
        }

        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    private AuthLoginVO buildLoginVOFromUser(User user, String token) {
        AuthLoginVO vo = new AuthLoginVO();
        vo.setId(user.getId());
        vo.setToken(token);
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserRole(user.getUserRole());
        vo.setExpireTime(LocalDateTime.now().plusMinutes(30));

        AuthLoginVO.RoleInfo roleInfo = new AuthLoginVO.RoleInfo();
        if (UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            roleInfo.setPermissions(getAdminPermissions());
        }
        vo.setRoleInfo(roleInfo);

        return vo;
    }

    private AuthLoginVO buildLoginVOFromDoctor(Doctor doctor, String token) {
        AuthLoginVO vo = new AuthLoginVO();
        vo.setId(doctor.getId());
        vo.setToken(token);
        vo.setUserAccount(doctor.getUserAccount());
        vo.setUserName(doctor.getDoctorName());
        vo.setUserRole(UserConstant.DOCTOR_ROLE);
        vo.setExpireTime(LocalDateTime.now().plusMinutes(30));

        AuthLoginVO.RoleInfo roleInfo = new AuthLoginVO.RoleInfo();
        roleInfo.setDepartment(doctor.getDepartment());
        roleInfo.setTitle(doctor.getTitle());
        roleInfo.setConsultationFee(doctor.getConsultationFee());
        vo.setRoleInfo(roleInfo);

        return vo;
    }

    private List<String> getAdminPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add("user:manage");
        permissions.add("doctor:manage");
        permissions.add("system:manage");
        return permissions;
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