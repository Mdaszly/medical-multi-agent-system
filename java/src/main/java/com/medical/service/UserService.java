package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.entity.User;
import com.medical.model.vo.UserVO;
import com.medical.model.dto.user.UserUpdateRequest;

import java.util.Map;

public interface UserService {

    UserVO getUserById(Long id);

    User getUserEntityById(Long id);

    UserVO getCurrentUser();

    IPage<UserVO> listUserPage(long current, long pageSize, Map<String, Object> conditions);

    UserVO updateUser(Long id, UserUpdateRequest request);

    UserVO updateProfile(Long id, UserUpdateRequest request);

    void changePassword(Long id, String oldPassword, String newPassword);

    void bindPhone(Long id, String phone);

    void bindEmail(Long id, String email);

    void deleteUser(Long id);

    void disableUser(Long id);

    void enableUser(Long id);
}