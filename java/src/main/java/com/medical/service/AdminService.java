package com.medical.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medical.model.dto.admin.AdminQueryRequest;
import com.medical.model.dto.admin.AdminUpdateRequest;
import com.medical.model.entity.User;
import com.medical.model.vo.AdminVO;

public interface AdminService {

    AdminVO getAdminById(Long id);

    User getAdminEntityById(Long id);

    AdminVO getCurrentAdmin();

    IPage<AdminVO> listAdminPage(long current, long pageSize, AdminQueryRequest request);

    AdminVO addAdmin(AdminUpdateRequest request);

    AdminVO updateAdmin(Long id, AdminUpdateRequest request);

    void deleteAdmin(Long id);

    void resetPassword(Long id, String newPassword);
}