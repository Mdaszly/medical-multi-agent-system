package com.medical.service;

import com.medical.model.dto.AuthLoginRequest;
import com.medical.model.dto.AuthRegisterRequest;
import com.medical.model.vo.AuthLoginVO;
import com.medical.model.vo.AuthRegisterVO;

public interface AuthService {

    AuthRegisterVO register(AuthRegisterRequest request);

    AuthLoginVO login(AuthLoginRequest request);

    void logout();

    AuthLoginVO getCurrentUser();
}