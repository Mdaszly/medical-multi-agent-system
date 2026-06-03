package com.medical.service;

import cn.dev33.satoken.stp.StpUtil;
import com.medical.common.ErrorCode;
import com.medical.common.RedisCacheUtil;
import com.medical.common.RedissonLockUtil;
import com.medical.messaging.appointment.AppointmentEventBridge;
import com.medical.constant.AppointmentConstant;
import com.medical.exception.BusinessException;
import com.medical.mapper.*;
import com.medical.model.dto.appointment.AppointmentAddRequest;
import com.medical.model.dto.appointment.AppointmentCancelRequest;
import com.medical.model.dto.appointment.AppointmentQueryRequest;
import com.medical.model.entity.*;
import com.medical.model.vo.AppointmentSlotVO;
import com.medical.model.vo.AppointmentVO;
import com.medical.service.impl.AppointmentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.AopContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 预约服务单元测试
 *
 * <p>测试覆盖：
 * 1. 创建预约 - 正常流程、参数验证、并发控制
 * 2. 取消预约 - 正常流程、权限验证、状态检查
 * 3. 查询功能 - 用户预约、医生预约、分页查询
 * 4. 签到功能 - 状态转换验证
 *
 * <p>使用Mockito进行单元测试，不依赖外部数据库和Redis
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("预约服务单元测试")
class AppointmentServiceTest {

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AppointmentSlotMapper appointmentSlotMapper;

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BillMapper billMapper;

    @Mock
    private PrescriptionMapper prescriptionMapper;

    @Mock
    private RedissonLockUtil redissonLockUtil;

    @Mock
    private RedisCacheUtil redisCacheUtil;

    @Mock
    private AppointmentEventBridge appointmentEventBridge;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private User testUser;
    private Doctor testDoctor;
    private Schedule testSchedule;
    private AppointmentSlot testSlot;
    private Appointment testAppointment;

    private MockedStatic<StpUtil> stpUtilMock;

    private MockedStatic<AopContext> aopContextMock;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(6L);
        aopContextMock = mockStatic(AopContext.class);
        aopContextMock.when(AopContext::currentProxy).thenReturn(appointmentService);

        log.info("========== 测试前置准备 ==========");

        testUser = new User();
        testUser.setId(6L);
        testUser.setUserName("测试用户");
        testUser.setUserAccount("test_user");

        testDoctor = new Doctor();
        testDoctor.setId(1L);
        testDoctor.setDoctorName("李医生");
        testDoctor.setDepartment("心内科");
        testDoctor.setConsultationFee(new BigDecimal("20.00"));

        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setDoctorId(1L);
        testSchedule.setDoctorName("李医生");
        testSchedule.setDepartment("心内科");
        testSchedule.setScheduleDate(LocalDate.now().plusDays(1));
        testSchedule.setShiftType("MORNING");

        testSlot = new AppointmentSlot();
        testSlot.setId(1L);
        testSlot.setScheduleId(1L);
        testSlot.setTimeSlot("08:00-08:30");
        testSlot.setMaxSlots(5);
        testSlot.setAvailableSlots(3);
        testSlot.setLockedSlots(0);
        testSlot.setStatus("AVAILABLE");
        testSlot.setVersion(0);

        testAppointment = new Appointment();
        testAppointment.setId(1L);
        testAppointment.setAppointmentNo("APT20260515100000123456");
        testAppointment.setUserId(6L);
        testAppointment.setUserName("测试用户");
        testAppointment.setDoctorId(1L);
        testAppointment.setDoctorName("李医生");
        testAppointment.setDepartment("心内科");
        testAppointment.setScheduleId(1L);
        testAppointment.setScheduleDate(LocalDate.now().plusDays(1));
        testAppointment.setShiftType("MORNING");
        testAppointment.setTimeSlot("08:00-08:30");
        testAppointment.setConsultationFee(new BigDecimal("20.00"));
        testAppointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_PENDING);
        testAppointment.setCheckInStatus(false);
        testAppointment.setCreateTime(LocalDateTime.now());

        log.info("测试数据准备完成");
    }

    @AfterEach
    void tearDown() {
        log.info("========== 测试清理 ==========");
        if (stpUtilMock != null) {
            stpUtilMock.close();
        }
        if (aopContextMock != null) {
            aopContextMock.close();
        }
        reset(appointmentMapper, appointmentSlotMapper, scheduleMapper,
                doctorMapper, userMapper, billMapper, prescriptionMapper,
                redissonLockUtil, redisCacheUtil, appointmentEventBridge);
    }

    // ==================== 创建预约测试 ====================

    @Nested
    @DisplayName("创建预约测试")
    class CreateAppointmentTests {

        @Test
        @DisplayName("成功创建预约")
        void createAppointment_Success() {
            log.info("测试：成功创建预约");

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(1L);
            request.setTimeSlot("08:00-08:30");
            request.setRemark("首次就诊");

            when(redissonLockUtil.tryLock(anyString(), anyLong()))
                    .thenReturn(true);
            when(scheduleMapper.selectById(1L)).thenReturn(testSchedule);
            when(appointmentSlotMapper.selectByScheduleId(1L))
                    .thenReturn(Arrays.asList(testSlot));
            when(appointmentMapper.countByUserIdAndScheduleIdAndTimeSlot(anyLong(), anyLong(), anyString()))
                    .thenReturn(0);
            when(userMapper.selectById(6L)).thenReturn(testUser);
            when(doctorMapper.selectById(1L)).thenReturn(testDoctor);
            when(appointmentSlotMapper.decreaseAvailableSlotsWithLock(anyLong(), anyInt()))
                    .thenReturn(1);
            when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

            AppointmentVO result = appointmentService.createAppointment(request);

            assertNotNull(result);
            assertEquals("测试用户", result.getUserName());
            assertEquals("李医生", result.getDoctorName());
            assertEquals("08:00-08:30", result.getTimeSlot());
            assertEquals(AppointmentConstant.APPOINTMENT_STATUS_PENDING, result.getStatus());
            assertEquals("待就诊", result.getStatusText());

            verify(redissonLockUtil).tryLock(anyString(), eq(10L));
            verify(redissonLockUtil).unlock(anyString());
            verify(appointmentMapper).insert(any(Appointment.class));

            log.info("预约创建成功：{}", result.getAppointmentNo());
        }

        @Test
        @DisplayName("排班ID无效")
        void createAppointment_InvalidScheduleId() {
            log.info("测试：排班ID无效");

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(null);
            request.setTimeSlot("08:00-08:30");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.createAppointment(request));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("排班ID"));

            log.info("参数验证通过：{}", exception.getMessage());
        }

        @Test
        @DisplayName("时段为空")
        void createAppointment_EmptyTimeSlot() {
            log.info("测试：时段为空");

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(1L);
            request.setTimeSlot("");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.createAppointment(request));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("时段"));

            log.info("参数验证通过：{}", exception.getMessage());
        }

        @Test
        @DisplayName("获取分布式锁失败")
        void createAppointment_LockFailed() {
            log.info("测试：获取分布式锁失败");

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(1L);
            request.setTimeSlot("08:00-08:30");

            when(redissonLockUtil.tryLock(anyString(), anyLong()))
                    .thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.createAppointment(request));

            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("系统繁忙"));

            log.info("锁获取失败处理正确：{}", exception.getMessage());
        }

        @Test
        @DisplayName("排班不存在")
        void createAppointment_ScheduleNotFound() {
            log.info("测试：排班不存在");

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(999L);
            request.setTimeSlot("08:00-08:30");

            when(redissonLockUtil.tryLock(anyString(), anyLong()))
                    .thenReturn(true);
            when(scheduleMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.createAppointment(request));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("排班不存在"));

            verify(redissonLockUtil).unlock(anyString());

            log.info("排班验证通过：{}", exception.getMessage());
        }

        @Test
        @DisplayName("号源已满")
        void createAppointment_NoAvailableSlots() {
            log.info("测试：号源已满");

            testSlot.setAvailableSlots(0);

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(1L);
            request.setTimeSlot("08:00-08:30");

            when(redissonLockUtil.tryLock(anyString(), anyLong()))
                    .thenReturn(true);
            when(scheduleMapper.selectById(1L)).thenReturn(testSchedule);
            when(appointmentSlotMapper.selectByScheduleId(1L))
                    .thenReturn(Arrays.asList(testSlot));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.createAppointment(request));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("已约满"));

            verify(redissonLockUtil).unlock(anyString());

            log.info("号源验证通过：{}", exception.getMessage());
        }

        @Test
        @DisplayName("重复预约")
        void createAppointment_DuplicateAppointment() {
            log.info("测试：重复预约");

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(1L);
            request.setTimeSlot("08:00-08:30");

            when(redissonLockUtil.tryLock(anyString(), anyLong()))
                    .thenReturn(true);
            when(scheduleMapper.selectById(1L)).thenReturn(testSchedule);
            when(appointmentSlotMapper.selectByScheduleId(1L))
                    .thenReturn(Arrays.asList(testSlot));
            when(appointmentMapper.countByUserIdAndScheduleIdAndTimeSlot(anyLong(), anyLong(), anyString()))
                    .thenReturn(1);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.createAppointment(request));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("已预约"));

            verify(redissonLockUtil).unlock(anyString());

            log.info("重复预约验证通过：{}", exception.getMessage());
        }

        @Test
        @DisplayName("乐观锁更新失败（并发冲突）")
        void createAppointment_OptimisticLockFailed() {
            log.info("测试：乐观锁更新失败");

            AppointmentAddRequest request = new AppointmentAddRequest();
            request.setScheduleId(1L);
            request.setTimeSlot("08:00-08:30");

            when(redissonLockUtil.tryLock(anyString(), anyLong()))
                    .thenReturn(true);
            when(scheduleMapper.selectById(1L)).thenReturn(testSchedule);
            when(appointmentSlotMapper.selectByScheduleId(1L))
                    .thenReturn(Arrays.asList(testSlot));
            when(appointmentMapper.countByUserIdAndScheduleIdAndTimeSlot(anyLong(), anyLong(), anyString()))
                    .thenReturn(0);
            when(userMapper.selectById(6L)).thenReturn(testUser);
            when(doctorMapper.selectById(1L)).thenReturn(testDoctor);
            when(appointmentSlotMapper.decreaseAvailableSlotsWithLock(anyLong(), anyInt()))
                    .thenReturn(0);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.createAppointment(request));

            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("已被占用"));

            verify(redissonLockUtil).unlock(anyString());

            log.info("乐观锁验证通过：{}", exception.getMessage());
        }
    }

    // ==================== 取消预约测试 ====================

    @Nested
    @DisplayName("取消预约测试")
    class CancelAppointmentTests {

        @Test
        @DisplayName("成功取消预约")
        void cancelAppointment_Success() {
            log.info("测试：成功取消预约");

            AppointmentCancelRequest request = new AppointmentCancelRequest();
            request.setAppointmentId(1L);
            request.setCancelReason("时间冲突");

            when(appointmentMapper.selectById(1L)).thenReturn(testAppointment);
            when(appointmentSlotMapper.selectByScheduleId(1L))
                    .thenReturn(Arrays.asList(testSlot));
            when(appointmentMapper.updateById(any(Appointment.class))).thenReturn(1);
            when(appointmentSlotMapper.increaseAvailableSlots(anyLong())).thenReturn(1);

            assertDoesNotThrow(() -> appointmentService.cancelAppointment(request));

            verify(appointmentMapper).updateById(any(Appointment.class));
            verify(appointmentSlotMapper).increaseAvailableSlots(1L);

            log.info("预约取消成功");
        }

        @Test
        @DisplayName("预约不存在")
        void cancelAppointment_NotFound() {
            log.info("测试：预约不存在");

            AppointmentCancelRequest request = new AppointmentCancelRequest();
            request.setAppointmentId(999L);

            when(appointmentMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.cancelAppointment(request));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("不存在"));

            log.info("预约验证通过：{}", exception.getMessage());
        }

        @Test
        @DisplayName("预约状态不可取消")
        void cancelAppointment_InvalidStatus() {
            log.info("测试：预约状态不可取消");

            testAppointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN);

            AppointmentCancelRequest request = new AppointmentCancelRequest();
            request.setAppointmentId(1L);

            when(appointmentMapper.selectById(1L)).thenReturn(testAppointment);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.cancelAppointment(request));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("不可取消"));

            log.info("状态验证通过：{}", exception.getMessage());
        }
    }

    // ==================== 查询功能测试 ====================

    @Nested
    @DisplayName("查询功能测试")
    class QueryTests {

        @Test
        @DisplayName("根据ID获取预约")
        void getAppointmentById_Success() {
            log.info("测试：根据ID获取预约");

            when(appointmentMapper.selectById(1L)).thenReturn(testAppointment);

            AppointmentVO result = appointmentService.getAppointmentById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("测试用户", result.getUserName());
            assertEquals("李医生", result.getDoctorName());

            log.info("预约查询成功");
        }

        @Test
        @DisplayName("获取不存在的预约")
        void getAppointmentById_NotFound() {
            log.info("测试：获取不存在的预约");

            when(appointmentMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.getAppointmentById(999L));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            log.info("空值验证通过");
        }

        @Test
        @DisplayName("获取号源列表")
        void getAppointmentSlotsBySchedule_Success() {
            log.info("测试：获取号源列表");

            AppointmentSlot slot2 = new AppointmentSlot();
            slot2.setId(2L);
            slot2.setScheduleId(1L);
            slot2.setTimeSlot("09:00-09:30");
            slot2.setMaxSlots(5);
            slot2.setAvailableSlots(0);
            slot2.setStatus("SOLD_OUT");

            when(appointmentSlotMapper.selectByScheduleId(1L))
                    .thenReturn(Arrays.asList(testSlot, slot2));

            List<AppointmentSlotVO> result = appointmentService.getAppointmentSlotsBySchedule(1L);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("08:00-08:30", result.get(0).getTimeSlot());
            assertEquals("AVAILABLE", result.get(0).getStatus());
            assertEquals("09:00-09:30", result.get(1).getTimeSlot());
            assertEquals("SOLD_OUT", result.get(1).getStatus());

            log.info("号源列表查询成功，共 {} 条", result.size());
        }
    }

    // ==================== 签到功能测试 ====================

    @Nested
    @DisplayName("签到功能测试")
    class CheckInTests {

        @Test
        @DisplayName("成功签到")
        void checkInAppointment_Success() {
            log.info("测试：成功签到");

            when(appointmentMapper.selectById(1L)).thenReturn(testAppointment);
            when(appointmentMapper.checkInIfPending(
                    1L,
                    AppointmentConstant.APPOINTMENT_STATUS_PENDING,
                    AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN)).thenReturn(1);

            Appointment checkedIn = new Appointment();
            checkedIn.setId(testAppointment.getId());
            checkedIn.setUserId(testAppointment.getUserId());
            checkedIn.setDoctorId(testAppointment.getDoctorId());
            checkedIn.setStatus(AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN);
            when(appointmentMapper.selectById(1L)).thenReturn(testAppointment, checkedIn);

            assertDoesNotThrow(() -> appointmentService.checkInAppointment(1L));

            verify(appointmentMapper).checkInIfPending(
                    1L,
                    AppointmentConstant.APPOINTMENT_STATUS_PENDING,
                    AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN);
            verify(appointmentEventBridge).publishCheckedIn(any(Appointment.class),
                    eq(AppointmentConstant.APPOINTMENT_STATUS_PENDING));

            log.info("签到成功");
        }

        @Test
        @DisplayName("预约状态不可签到")
        void checkInAppointment_InvalidStatus() {
            log.info("测试：预约状态不可签到");

            testAppointment.setStatus(AppointmentConstant.APPOINTMENT_STATUS_COMPLETED);

            when(appointmentMapper.selectById(1L)).thenReturn(testAppointment);
            when(appointmentMapper.checkInIfPending(
                    1L,
                    AppointmentConstant.APPOINTMENT_STATUS_PENDING,
                    AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN)).thenReturn(0);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> appointmentService.checkInAppointment(1L));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("不可签到"));

            log.info("状态验证通过：{}", exception.getMessage());
        }
    }

    // ==================== 状态枚举测试 ====================

    @Nested
    @DisplayName("状态枚举测试")
    class StatusTests {

        @Test
        @DisplayName("预约状态常量验证")
        void appointmentStatusConstants() {
            assertEquals(0, AppointmentConstant.APPOINTMENT_STATUS_PENDING);
            assertEquals(1, AppointmentConstant.APPOINTMENT_STATUS_CHECKED_IN);
            assertEquals(2, AppointmentConstant.APPOINTMENT_STATUS_IN_CONSULTATION);
            assertEquals(3, AppointmentConstant.APPOINTMENT_STATUS_COMPLETED);
            assertEquals(4, AppointmentConstant.APPOINTMENT_STATUS_CANCELLED);
            assertEquals(5, AppointmentConstant.APPOINTMENT_STATUS_EXPIRED);
            assertEquals(6, AppointmentConstant.APPOINTMENT_STATUS_SETTLED);
        }

        @Test
        @DisplayName("号源状态常量验证")
        void slotStatusConstants() {
            assertEquals("AVAILABLE", AppointmentConstant.SLOT_STATUS_AVAILABLE);
            assertEquals("LOCKED", AppointmentConstant.SLOT_STATUS_LOCKED);
            assertEquals("SOLD_OUT", AppointmentConstant.SLOT_STATUS_SOLD_OUT);
        }

        @Test
        @DisplayName("预约编号格式验证")
        void appointmentNoFormat() {
            assertEquals("APT", AppointmentConstant.APPOINTMENT_NO_PREFIX);
            assertTrue(AppointmentConstant.APPOINTMENT_NO_PREFIX.length() > 0);
        }
    }
}
