package com.medical.service;

import com.medical.model.dto.user.UserLoginRequest;
import com.medical.model.dto.user.UserRegisterRequest;
import com.medical.model.entity.User;
import com.medical.model.vo.UserLoginVO;
import com.medical.model.vo.UserVO;

public interface UserService {

    Long register(UserRegisterRequest request);

    UserLoginVO login(UserLoginRequest request);

    void logout();

    UserVO getCurrentUser();

    UserVO getUserById(Long id);

    User getUserEntityById(Long id);
}
