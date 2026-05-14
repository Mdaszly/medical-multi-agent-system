package com.medical.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.ErrorCode;
import com.medical.constant.UserConstant;
import com.medical.exception.BusinessException;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.DoctorMapper;
import com.medical.model.dto.doctor.DoctorQueryRequest;
import com.medical.model.dto.doctor.DoctorUpdateRequest;
import com.medical.model.entity.Doctor;
import com.medical.model.vo.DoctorVO;
import com.medical.service.DoctorCacheManager;
import com.medical.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 医生服务实现类
 * 
 * <p>负责医生信息的CRUD操作，已集成Redis缓存优化。
 * 核心缓存优化点：
 * 1. 按科室查询医生列表（热点接口）使用Redis缓存
 * 2. 医生数据变更时自动删除相关缓存，保证数据一致性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;
    
    /**
     * 医生缓存管理器，提供缓存查询、更新、删除等功能
     */
    private final DoctorCacheManager doctorCacheManager;

    @Override
    public DoctorVO getDoctorById(Long id) {
        Doctor doctor = getDoctorEntityById(id);
        return DoctorVO.fromEntity(doctor);
    }

    @Override
    public Doctor getDoctorEntityById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "医生ID无效");
        Doctor doctor = doctorMapper.selectById(id);
        ThrowUtils.throwIf(doctor == null, ErrorCode.USER_NOT_FOUND);
        return doctor;
    }

    @Override
    public DoctorVO getCurrentDoctor() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long doctorId = StpUtil.getLoginIdAsLong();
        return getDoctorById(doctorId);
    }

    @Override
    public IPage<DoctorVO> listDoctorPage(long current, long pageSize, DoctorQueryRequest request) {
        log.info("List doctor page: current={}, pageSize={}, request={}", current, pageSize, request);
        
        Page<Doctor> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        
        if (request != null) {
            if (StringUtils.hasText(request.getDoctorNo())) {
                wrapper.eq(Doctor::getDoctorNo, request.getDoctorNo());
            }
            if (StringUtils.hasText(request.getDoctorName())) {
                wrapper.like(Doctor::getDoctorName, request.getDoctorName());
            }
            if (StringUtils.hasText(request.getDepartment())) {
                wrapper.eq(Doctor::getDepartment, request.getDepartment());
            }
            if (StringUtils.hasText(request.getTitle())) {
                wrapper.eq(Doctor::getTitle, request.getTitle());
            }
            if (request.getWorkStatus() != null) {
                wrapper.eq(Doctor::getWorkStatus, request.getWorkStatus());
            }
        }
        
        wrapper.eq(Doctor::getIsDelete, UserConstant.NOT_DELETED)
               .orderByDesc(Doctor::getCreateTime);
        
        IPage<Doctor> doctorPage = doctorMapper.selectPage(page, wrapper);
        
        return doctorPage.convert(DoctorVO::fromEntity);
    }

    /**
     * 根据科室名称查询医生列表（带缓存）
     * 
     * <p>这是核心的缓存优化接口，查询流程：
     * 1. 调用DoctorCacheManager获取医生列表
     * 2. 如果缓存命中，直接返回缓存数据
     * 3. 如果缓存未命中，执行loadDoctorsFromDatabase加载数据
     * 4. 加载的数据会自动写入缓存
     * 
     * @param department 科室名称
     * @return 医生列表
     */
    @Override
    public List<DoctorVO> listDoctorByDepartment(String department) {
        log.info("List doctor by department: {}", department);

        ThrowUtils.throwIf(!StringUtils.hasText(department), ErrorCode.PARAM_ERROR, "科室不能为空");

        // 使用缓存管理器获取医生列表
        // 第二个参数是一个Supplier，当缓存未命中时会执行这个函数加载数据
        return doctorCacheManager.getDoctorListByDepartment(department, () -> loadDoctorsFromDatabase(department));
    }

    /**
     * 从数据库加载科室医生列表
     * 
     * <p>这是一个私有方法，仅在缓存未命中时被调用。
     * 查询条件：
     * - 指定科室
     * - 工作状态为在线
     * - 未被删除
     * - 按职称排序
     */
    private List<DoctorVO> loadDoctorsFromDatabase(String department) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getDepartment, department)
               .eq(Doctor::getWorkStatus, UserConstant.DOCTOR_STATUS_ONLINE)
               .eq(Doctor::getIsDelete, UserConstant.NOT_DELETED)
               .orderByAsc(Doctor::getTitle);

        List<Doctor> doctors = doctorMapper.selectList(wrapper);

        return doctors.stream()
                .map(DoctorVO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 更新医生信息
     * 
     * <p>更新完成后会检查科室是否变更，如果变更则删除新旧科室的缓存，
     * 确保下次查询时能获取最新数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorVO updateDoctor(Long id, DoctorUpdateRequest request) {
        log.info("Update doctor: id={}, request={}", id, request);

        Doctor doctor = getDoctorEntityById(id);
        // 保存旧科室，用于判断是否需要删除缓存
        String oldDepartment = doctor.getDepartment();

        updateDoctorEntity(doctor, request);
        doctor.setUpdateTime(LocalDateTime.now());

        int result = doctorMapper.updateById(doctor);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "更新失败");

        // 如果科室发生变化，删除相关缓存
        evictCacheIfNeeded(oldDepartment, doctor.getDepartment());

        log.info("Doctor updated successfully: id={}", id);
        return DoctorVO.fromEntity(doctor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorVO updateProfile(Long id, DoctorUpdateRequest request) {
        log.info("Update doctor profile: id={}", id);
        
        Doctor doctor = getDoctorEntityById(id);
        
        if (request.getDoctorName() != null) {
            doctor.setDoctorName(request.getDoctorName());
        }
        if (request.getGender() != null) {
            doctor.setGender(request.getGender());
        }
        if (request.getSpecialty() != null) {
            doctor.setSpecialty(request.getSpecialty());
        }
        if (request.getDescription() != null) {
            doctor.setDescription(request.getDescription());
        }
        
        doctor.setUpdateTime(LocalDateTime.now());
        
        int result = doctorMapper.updateById(doctor);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "更新失败");
        
        log.info("Doctor profile updated successfully: id={}", id);
        return DoctorVO.fromEntity(doctor);
    }

    /**
     * 删除医生（逻辑删除）
     * 
     * <p>删除后会删除该医生所在科室的缓存，确保数据一致性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoctor(Long id) {
        log.info("Delete doctor: id={}", id);

        Doctor doctor = getDoctorEntityById(id);
        String department = doctor.getDepartment();

        doctor.setIsDelete(UserConstant.IS_DELETED);
        doctor.setUpdateTime(LocalDateTime.now());

        int result = doctorMapper.updateById(doctor);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "删除失败");

        // 删除该科室的缓存
        doctorCacheManager.evictDepartmentCache(department);

        log.info("Doctor deleted successfully: id={}", id);
    }

    /**
     * 禁用医生账号
     * 
     * <p>禁用后医生状态变为离线，会删除相关科室缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableDoctor(Long id) {
        log.info("Disable doctor: id={}", id);

        Doctor doctor = getDoctorEntityById(id);
        String department = doctor.getDepartment();

        doctor.setWorkStatus(UserConstant.DOCTOR_STATUS_OFFLINE);
        doctor.setUpdateTime(LocalDateTime.now());

        int result = doctorMapper.updateById(doctor);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "禁用失败");

        // 删除该科室的缓存
        doctorCacheManager.evictDepartmentCache(department);

        log.info("Doctor disabled successfully: id={}", id);
    }

    /**
     * 启用医生账号
     * 
     * <p>启用后医生状态变为在线，会删除相关科室缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableDoctor(Long id) {
        log.info("Enable doctor: id={}", id);

        Doctor doctor = getDoctorEntityById(id);
        String department = doctor.getDepartment();

        doctor.setWorkStatus(UserConstant.DOCTOR_STATUS_ONLINE);
        doctor.setUpdateTime(LocalDateTime.now());

        int result = doctorMapper.updateById(doctor);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "启用失败");

        // 删除该科室的缓存
        doctorCacheManager.evictDepartmentCache(department);

        log.info("Doctor enabled successfully: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorVO addDoctor(DoctorUpdateRequest request) {
        log.info("Add doctor: name={}", request.getDoctorName());
        
        ThrowUtils.throwIf(!StringUtils.hasText(request.getDoctorName()), 
                ErrorCode.PARAM_ERROR, "医生姓名不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(request.getDepartment()), 
                ErrorCode.PARAM_ERROR, "科室不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(request.getTitle()), 
                ErrorCode.PARAM_ERROR, "职称不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(request.getLicenseNo()), 
                ErrorCode.PARAM_ERROR, "执业医师证号不能为空");
        
        checkLicenseNoExists(request.getLicenseNo());
        
        Doctor doctor = new Doctor();
        doctor.setDoctorNo("DOC" + System.currentTimeMillis());
        updateDoctorEntity(doctor, request);
        doctor.setWorkStatus(UserConstant.DOCTOR_STATUS_ONLINE);
        
        int result = doctorMapper.insert(doctor);
        ThrowUtils.throwIf(result == 0, ErrorCode.SYSTEM_ERROR, "添加失败");
        
        log.info("Doctor added successfully: id={}, name={}", doctor.getId(), request.getDoctorName());
        return DoctorVO.fromEntity(doctor);
    }

    private void updateDoctorEntity(Doctor doctor, DoctorUpdateRequest request) {
        if (request.getDoctorName() != null) {
            doctor.setDoctorName(request.getDoctorName());
        }
        if (request.getGender() != null) {
            doctor.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            validatePhone(request.getPhone());
            checkPhoneExists(request.getPhone(), doctor.getId());
            doctor.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
            checkEmailExists(request.getEmail(), doctor.getId());
            doctor.setEmail(request.getEmail());
        }
        if (request.getDepartment() != null) {
            doctor.setDepartment(request.getDepartment());
        }
        if (request.getTitle() != null) {
            doctor.setTitle(request.getTitle());
        }
        if (request.getSpecialty() != null) {
            doctor.setSpecialty(request.getSpecialty());
        }
        if (request.getLicenseNo() != null) {
            checkLicenseNoExists(request.getLicenseNo());
            doctor.setLicenseNo(request.getLicenseNo());
        }
        if (request.getHospitalName() != null) {
            doctor.setHospitalName(request.getHospitalName());
        }
        if (request.getConsultationFee() != null) {
            doctor.setConsultationFee(request.getConsultationFee());
        }
        if (request.getDescription() != null) {
            doctor.setDescription(request.getDescription());
        }
        if (request.getWorkStatus() != null) {
            doctor.setWorkStatus(request.getWorkStatus());
        }
    }

    private void validatePhone(String phone) {
        ThrowUtils.throwIf(!phone.matches("^1[3-9]\\d{9}$"), ErrorCode.PARAM_ERROR, "手机号格式不正确");
    }

    private void validateEmail(String email) {
        ThrowUtils.throwIf(!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"), 
                ErrorCode.PARAM_ERROR, "邮箱格式不正确");
    }

    private void checkPhoneExists(String phone, Long excludeId) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getPhone, phone)
               .ne(excludeId != null, Doctor::getId, excludeId)
               .eq(Doctor::getIsDelete, UserConstant.NOT_DELETED);
        if (doctorMapper.exists(wrapper)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该手机号已被使用");
        }
    }

    private void checkEmailExists(String email, Long excludeId) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getEmail, email)
               .ne(excludeId != null, Doctor::getId, excludeId)
               .eq(Doctor::getIsDelete, UserConstant.NOT_DELETED);
        if (doctorMapper.exists(wrapper)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该邮箱已被使用");
        }
    }

    private void checkLicenseNoExists(String licenseNo) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getLicenseNo, licenseNo)
               .eq(Doctor::getIsDelete, UserConstant.NOT_DELETED);
        if (doctorMapper.exists(wrapper)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该执业证书编号已存在");
        }
    }

    /**
     * 根据科室变更情况决定是否删除缓存
     * 
     * <p>当医生的科室发生变化时，需要删除新旧科室的缓存，
     * 确保缓存数据的一致性。
     */
    private void evictCacheIfNeeded(String oldDepartment, String newDepartment) {
        if (oldDepartment != null) {
            doctorCacheManager.evictDepartmentCache(oldDepartment);
        }
        if (newDepartment != null && !newDepartment.equals(oldDepartment)) {
            doctorCacheManager.evictDepartmentCache(newDepartment);
        }
    }
}