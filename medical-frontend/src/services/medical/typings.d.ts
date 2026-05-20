declare namespace API {
  type AdminIdRequest = {
    id?: number;
  };

  type AdminQueryRequest = {
    /** 当前页码 */
    current?: number;
    /** 每页数量 */
    pageSize?: number;
    /** 管理员账号（模糊查询） */
    userAccount?: string;
    /** 管理员姓名（模糊查询） */
    userName?: string;
    /** 账号状态：0-禁用，1-正常 */
    userStatus?: number;
  };

  type AdminUpdateRequest = {
    /** 管理员账号 */
    userAccount?: string;
    /** 管理员姓名 */
    userName?: string;
    /** 密码 */
    password?: string;
    /** 手机号 */
    phone?: string;
    /** 邮箱 */
    email?: string;
    /** 账号状态：0-禁用，1-正常 */
    userStatus?: number;
  };

  type AdminUpdateWithIdRequest = {
    /** 管理员账号 */
    userAccount?: string;
    /** 管理员姓名 */
    userName?: string;
    /** 密码 */
    password?: string;
    /** 手机号 */
    phone?: string;
    /** 邮箱 */
    email?: string;
    /** 账号状态：0-禁用，1-正常 */
    userStatus?: number;
    id?: number;
  };

  type AdminVO = {
    id?: number;
    userAccount?: string;
    userName?: string;
    phone?: string;
    email?: string;
    userRole?: string;
    userStatus?: number;
    lastLoginTime?: string;
    createTime?: string;
  };

  type AgentTraceVO = {
    agent?: string;
    action?: string;
    detail?: string;
  };

  type AnalyzeRequest = {
    patientDescription: string;
  };

  type AppointmentAddRequest = {
    scheduleId?: number;
    timeSlot?: string;
    remark?: string;
  };

  type AppointmentCancelRequest = {
    appointmentId?: number;
    cancelReason?: string;
  };

  type AppointmentQueryRequest = {
    userId?: number;
    doctorId?: number;
    department?: string;
    scheduleDate?: string;
    status?: number;
    current?: number;
    pageSize?: number;
  };

  type AppointmentSlotVO = {
    id?: number;
    scheduleId?: number;
    doctorId?: number;
    doctorName?: string;
    timeSlot?: string;
    timeStart?: string;
    timeEnd?: string;
    maxSlots?: number;
    availableSlots?: number;
    lockedSlots?: number;
    status?: string;
    createTime?: string;
    updateTime?: string;
  };

  type AppointmentVO = {
    id?: number;
    appointmentNo?: string;
    userId?: number;
    userName?: string;
    doctorId?: number;
    doctorName?: string;
    department?: string;
    scheduleId?: number;
    scheduleDate?: string;
    shiftType?: string;
    timeSlot?: string;
    consultationFee?: number;
    status?: number;
    statusText?: string;
    checkInTime?: string;
    checkInStatus?: boolean;
    cancelTime?: string;
    cancelReason?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  };

  type AuthLoginRequest = {
    userAccount: string;
    password: string;
  };

  type AuthLoginVO = {
    id?: number;
    token?: string;
    userAccount?: string;
    userName?: string;
    userRole?: string;
    expireTime?: string;
    roleInfo?: RoleInfo;
  };

  type AuthRegisterRequest = {
    userAccount: string;
    userPassword: string;
    checkPassword: string;
    userName?: string;
    phone?: string;
    email?: string;
    userRole: string;
    department?: string;
    title?: string;
    licenseNo?: string;
    consultationFee?: number;
  };

  type AuthRegisterVO = {
    id?: number;
    userAccount?: string;
    userName?: string;
    userRole?: string;
    createTime?: string;
  };

  type BaseResponseAdminVO = {
    code?: number;
    data?: AdminVO;
    message?: string;
  };

  type BaseResponseAppointmentSlotVO = {
    code?: number;
    data?: AppointmentSlotVO;
    message?: string;
  };

  type BaseResponseAppointmentVO = {
    code?: number;
    data?: AppointmentVO;
    message?: string;
  };

  type BaseResponseAuthLoginVO = {
    code?: number;
    data?: AuthLoginVO;
    message?: string;
  };

  type BaseResponseAuthRegisterVO = {
    code?: number;
    data?: AuthRegisterVO;
    message?: string;
  };

  type BaseResponseBigDecimal = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponseBillVO = {
    code?: number;
    data?: BillVO;
    message?: string;
  };

  type BaseResponseBoolean = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseChatMessageVO = {
    code?: number;
    data?: ChatMessageVO;
    message?: string;
  };

  type BaseResponseChatSessionHistoryVO = {
    code?: number;
    data?: ChatSessionHistoryVO;
    message?: string;
  };

  type BaseResponseChatSessionVO = {
    code?: number;
    data?: ChatSessionVO;
    message?: string;
  };

  type BaseResponseConsultVO = {
    code?: number;
    data?: ConsultVO;
    message?: string;
  };

  type BaseResponseDoctorVO = {
    code?: number;
    data?: DoctorVO;
    message?: string;
  };

  type BaseResponseDrugVO = {
    code?: number;
    data?: DrugVO;
    message?: string;
  };

  type BaseResponseFeeItemVO = {
    code?: number;
    data?: FeeItemVO;
    message?: string;
  };

  type BaseResponseHealthProfileVO = {
    code?: number;
    data?: HealthProfileVO;
    message?: string;
  };

  type BaseResponseIPageAdminVO = {
    code?: number;
    data?: IPageAdminVO;
    message?: string;
  };

  type BaseResponseIPageAppointmentVO = {
    code?: number;
    data?: IPageAppointmentVO;
    message?: string;
  };

  type BaseResponseIPageBillVO = {
    code?: number;
    data?: IPageBillVO;
    message?: string;
  };

  type BaseResponseIPageDoctorVO = {
    code?: number;
    data?: IPageDoctorVO;
    message?: string;
  };

  type BaseResponseIPagePrescriptionVO = {
    code?: number;
    data?: IPagePrescriptionVO;
    message?: string;
  };

  type BaseResponseIPageScheduleVO = {
    code?: number;
    data?: IPageScheduleVO;
    message?: string;
  };

  type BaseResponseIPageUserVO = {
    code?: number;
    data?: IPageUserVO;
    message?: string;
  };

  type BaseResponseListAppointmentSlotVO = {
    code?: number;
    data?: AppointmentSlotVO[];
    message?: string;
  };

  type BaseResponseListAppointmentVO = {
    code?: number;
    data?: AppointmentVO[];
    message?: string;
  };

  type BaseResponseListBillVO = {
    code?: number;
    data?: BillVO[];
    message?: string;
  };

  type BaseResponseListChatMessageVO = {
    code?: number;
    data?: ChatMessageVO[];
    message?: string;
  };

  type BaseResponseListDoctorVO = {
    code?: number;
    data?: DoctorVO[];
    message?: string;
  };

  type BaseResponseListDrugWithPriceVO = {
    code?: number;
    data?: DrugWithPriceVO[];
    message?: string;
  };

  type BaseResponseListFeeItemVO = {
    code?: number;
    data?: FeeItemVO[];
    message?: string;
  };

  type BaseResponseListMapStringString = {
    code?: number;
    data?: Record<string, any>[];
    message?: string;
  };

  type BaseResponseListPaymentVO = {
    code?: number;
    data?: PaymentVO[];
    message?: string;
  };

  type BaseResponseListPrescriptionVO = {
    code?: number;
    data?: PrescriptionVO[];
    message?: string;
  };

  type BaseResponseListScheduleVO = {
    code?: number;
    data?: ScheduleVO[];
    message?: string;
  };

  type BaseResponseMapStringObject = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type BaseResponsePaymentVO = {
    code?: number;
    data?: PaymentVO;
    message?: string;
  };

  type BaseResponsePrescriptionVO = {
    code?: number;
    data?: PrescriptionVO;
    message?: string;
  };

  type BaseResponseScheduleVO = {
    code?: number;
    data?: ScheduleVO;
    message?: string;
  };

  type BaseResponseString = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseUserVO = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type BaseResponseVoid = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type BillGenerateRequest = {
    userId?: number;
    appointmentId?: number;
    prescriptionId?: number;
  };

  type BillVO = {
    id?: number;
    billNo?: string;
    userId?: number;
    appointmentId?: number;
    totalAmount?: number;
    discountAmount?: number;
    insuranceAmount?: number;
    selfPayAmount?: number;
    paidAmount?: number;
    status?: string;
    payTime?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
    feeItems?: FeeItemVO[];
  };

  type BindEmailRequest = {
    email?: string;
  };

  type BindPhoneRequest = {
    phone?: string;
  };

  type cancelPrescriptionParams = {
    /** 处方ID */
    prescriptionId: number;
  };

  type ChangePasswordRequest = {
    oldPassword?: string;
    newPassword?: string;
  };

  type ChatMessageSaveRequest = {
    sessionId?: string;
    role?: string;
    content?: string;
    agentType?: string;
    riskLevel?: string;
    metadataJson?: string;
  };

  type ChatMessageVO = {
    id?: number;
    sessionId?: string;
    role?: string;
    content?: string;
    agentType?: string;
    riskLevel?: string;
    metadataJson?: string;
    createTime?: string;
  };

  type ChatSessionCreateRequest = {
    scene?: string;
    title?: string;
  };

  type ChatSessionHistoryVO = {
    today?: ChatSessionVO[];
    last30Days?: ChatSessionVO[];
    lastYear?: ChatSessionVO[];
    olderThanYear?: ChatSessionVO[];
  };

  type ChatSessionTitleUpdateRequest = {
    sessionId?: string;
    title?: string;
  };

  type ChatSessionVO = {
    sessionId?: string;
    title?: string;
    scene?: string;
    createTime?: string;
    updateTime?: string;
  };

  type checkInAppointmentParams = {
    /** 预约ID */
    appointmentId: number;
  };

  type checkScheduleConflictParams = {
    /** 医生ID */
    doctorId: number;
    /** 排班日期 */
    scheduleDate: string;
    /** 班次类型 */
    shiftType: string;
  };

  type ClinicalState = {
    rawInput?: string;
    patientInfo?: Record<string, any>;
    diagnosis?: Record<string, any>;
    needsMoreInfo?: boolean;
    treatmentPlan?: Record<string, any>;
    codingResult?: Record<string, any>;
    auditResult?: Record<string, any>;
    currentAgent?: string;
    errors?: string[];
    extensions?: Record<string, any>;
  };

  type ConsultRequest = {
    sessionId?: string;
    scene?: string;
    question: string;
    patientContext?: Record<string, any>;
  };

  type ConsultVO = {
    sessionId?: string;
    answer?: string;
    riskLevel?: string;
    recommendedDepartment?: string;
    conclusion?: string;
    reasoning?: string;
    redFlags?: string[];
    nextQuestions?: string[];
    careAdvice?: string[];
    evidenceSummary?: string;
    disclaimer?: string;
    agentType?: string;
    agentTrace?: AgentTraceVO[];
    errors?: string[];
    graphEvidence?: SymptomDiagnosisRowVO[];
    groundingStatus?: string;
  };

  type createConstraintParams = {
    label: string;
    property: string;
  };

  type createIndexParams = {
    label: string;
    property: string;
  };

  type deleteDrugParams = {
    /** 药品ID */
    id: number;
  };

  type deleteScheduleParams = {
    /** 排班ID */
    id: number;
  };

  type deleteSessionParams = {
    sessionId: string;
  };

  type deleteSlotParams = {
    /** 号源ID */
    id: number;
  };

  type dispensePrescriptionParams = {
    /** 处方ID */
    prescriptionId: number;
  };

  type DoctorIdRequest = {
    id?: number;
  };

  type DoctorQueryRequest = {
    /** 当前页码 */
    current?: number;
    /** 每页数量 */
    pageSize?: number;
    /** 医生编号 */
    doctorNo?: string;
    /** 医生姓名（模糊查询） */
    doctorName?: string;
    /** 科室 */
    department?: string;
    /** 职称 */
    title?: string;
    /** 工作状态：0-休假，1-在岗，2-离职 */
    workStatus?: number;
  };

  type DoctorUpdateRequest = {
    /** 医生姓名 */
    doctorName?: string;
    /** 性别：0-未知，1-男，2-女 */
    gender?: number;
    /** 手机号 */
    phone?: string;
    /** 邮箱 */
    email?: string;
    /** 科室 */
    department?: string;
    /** 职称 */
    title?: string;
    /** 擅长领域 */
    specialty?: string;
    /** 执业医师证号 */
    licenseNo?: string;
    /** 所属医院名称 */
    hospitalName?: string;
    /** 挂号费用 */
    consultationFee?: number;
    /** 医生简介 */
    description?: string;
    /** 工作状态：0-休假，1-在岗，2-离职 */
    workStatus?: number;
  };

  type DoctorUpdateWithIdRequest = {
    /** 医生姓名 */
    doctorName?: string;
    /** 性别：0-未知，1-男，2-女 */
    gender?: number;
    /** 手机号 */
    phone?: string;
    /** 邮箱 */
    email?: string;
    /** 科室 */
    department?: string;
    /** 职称 */
    title?: string;
    /** 擅长领域 */
    specialty?: string;
    /** 执业医师证号 */
    licenseNo?: string;
    /** 所属医院名称 */
    hospitalName?: string;
    /** 挂号费用 */
    consultationFee?: number;
    /** 医生简介 */
    description?: string;
    /** 工作状态：0-休假，1-在岗，2-离职 */
    workStatus?: number;
    id?: number;
  };

  type DoctorVO = {
    id?: number;
    doctorNo?: string;
    doctorName?: string;
    department?: string;
    title?: string;
    specialty?: string;
    phone?: string;
    email?: string;
    hospitalName?: string;
    consultationFee?: number;
    description?: string;
    workStatus?: number;
    createTime?: string;
  };

  type DrugAddRequest = {
    drugCode?: string;
    drugName?: string;
    genericName?: string;
    tradeName?: string;
    specification?: string;
    unit?: string;
    manufacturer?: string;
    category?: string;
    categoryCode?: string;
    dosageForm?: string;
    prescriptionFlag?: boolean;
    remark?: string;
  };

  type DrugQueryRequest = {
    drugCode?: string;
    drugName?: string;
    categoryCode?: string;
    status?: number;
    current?: number;
    pageSize?: number;
  };

  type DrugUpdateRequest = {
    id?: number;
    drugName?: string;
    genericName?: string;
    tradeName?: string;
    specification?: string;
    unit?: string;
    manufacturer?: string;
    category?: string;
    categoryCode?: string;
    dosageForm?: string;
    prescriptionFlag?: boolean;
    status?: number;
    remark?: string;
  };

  type DrugVO = {
    id?: number;
    drugCode?: string;
    drugName?: string;
    genericName?: string;
    tradeName?: string;
    specification?: string;
    unit?: string;
    manufacturer?: string;
    category?: string;
    categoryCode?: string;
    dosageForm?: string;
    prescriptionFlag?: boolean;
    status?: number;
    createTime?: string;
    remark?: string;
  };

  type DrugWithPriceVO = {
    id?: number;
    drugCode?: string;
    drugName?: string;
    specification?: string;
    unit?: string;
    retailPrice?: number;
    insurancePrice?: number;
  };

  type exportBillParams = {
    /** 用户ID */
    userId?: number;
    /** 状态 */
    status?: string;
  };

  type FeeCalculationRequest = {
    drugCode?: string;
    drugName?: string;
    unitPrice?: number;
    quantity?: number;
  };

  type FeeItemVO = {
    id?: number;
    feeItemNo?: string;
    userId?: number;
    appointmentId?: number;
    prescriptionId?: number;
    billId?: number;
    itemType?: string;
    itemName?: string;
    itemCode?: string;
    quantity?: number;
    unitPrice?: number;
    totalAmount?: number;
    discountAmount?: number;
    actualAmount?: number;
    insuranceAmount?: number;
    selfPayAmount?: number;
    status?: string;
    settleFlag?: boolean;
    createTime?: string;
    updateTime?: string;
  };

  type findDiagnosesParams = {
    symptomName: string;
  };

  type findDrugIndicationsParams = {
    drugName: string;
  };

  type findNodeByNameParams = {
    label: string;
    name: string;
  };

  type findNodeRelationsByQueryParams = {
    label: string;
    name: string;
    depth?: number;
  };

  type findNodeRelationsParams = {
    label: string;
    name: string;
    depth?: number;
  };

  type findPathsParams = {
    sourceLabel: string;
    sourceName: string;
    targetLabel: string;
    targetName: string;
    maxDepth?: number;
  };

  type generateBillByAppointmentParams = {
    /** 预约ID */
    appointmentId: number;
  };

  type generateDefaultSlotsParams = {
    /** 排班ID */
    scheduleId: number;
  };

  type getAppointmentByIdParams = {
    /** 预约ID */
    id: number;
  };

  type getAppointmentSlotsParams = {
    /** 排班ID */
    scheduleId: number;
  };

  type getBillByIdParams = {
    /** 账单ID */
    id: number;
  };

  type getBillByNoParams = {
    /** 账单编号 */
    billNo: string;
  };

  type getByAppointmentIdParams = {
    /** 预约ID */
    appointmentId: number;
  };

  type getCurrentPriceByCodeParams = {
    /** 药品编码 */
    drugCode: string;
    /** 价格类型 */
    priceType?: string;
  };

  type getCurrentPriceParams = {
    /** 药品ID */
    drugId: number;
    /** 价格类型 */
    priceType?: string;
  };

  type getDoctorLoadBalanceParams = {
    /** 科室名称 */
    department: string;
  };

  type getDrugByCodeParams = {
    /** 药品编码 */
    drugCode: string;
  };

  type getDrugParams = {
    /** 药品ID */
    id: number;
  };

  type getFeeItemByIdParams = {
    /** 费用项ID */
    id: number;
  };

  type getHealthProfileParams = {
    /** 用户ID */
    userId?: number;
  };

  type getImportTaskStatusParams = {
    taskId: string;
  };

  type getPaymentByIdParams = {
    /** 支付ID */
    id: number;
  };

  type getPaymentByNoParams = {
    /** 支付编号 */
    paymentNo: string;
  };

  type getPaymentStatusParams = {
    /** 支付ID */
    paymentId: number;
  };

  type getPrescriptionByIdParams = {
    /** 处方ID */
    id: number;
  };

  type getPrescriptionByNoParams = {
    /** 处方编号 */
    prescriptionNo: string;
  };

  type getScheduleByIdParams = {
    /** 排班ID */
    id: number;
  };

  type getSlotByIdParams = {
    /** 号源ID */
    id: number;
  };

  type getSlotsByScheduleParams = {
    /** 排班ID */
    scheduleId: number;
  };

  type getUnsettledAmountParams = {
    /** 用户ID */
    userId: number;
  };

  type HealthProfile = {
    id?: number;
    userId?: number;
    userName?: string;
    chronicDiseases?: string;
    allergyHistory?: string;
    medicationHistory?: string;
    familyHistory?: string;
    surgicalHistory?: string;
    vaccinationHistory?: string;
    physicalExam?: string;
    height?: number;
    weight?: number;
    bloodType?: string;
    bloodPressure?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  };

  type HealthProfileVO = {
    id?: number;
    userId?: number;
    userName?: string;
    chronicDiseases?: string;
    allergyHistory?: string;
    medicationHistory?: string;
    familyHistory?: string;
    surgicalHistory?: string;
    vaccinationHistory?: string;
    physicalExam?: string;
    height?: number;
    weight?: number;
    bloodType?: string;
    bloodPressure?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  };

  type ImportConfig = {
    skipHeader?: boolean;
    delimiter?: string;
    fieldMapping?: Record<string, any>;
    encoding?: string;
    batchSize?: number;
  };

  type importFileParams = {
    entityType: string;
    skipHeader?: boolean;
    delimiter?: string;
  };

  type ImportTaskDTO = {
    taskId?: string;
    filePath?: string;
    fileType?: string;
    entityType?: string;
    totalRecords?: number;
    processedRecords?: number;
    successCount?: number;
    failureCount?: number;
    status?: string;
    startTime?: string;
    endTime?: string;
    errors?: string[];
    config?: ImportConfig;
  };

  type IPageAdminVO = {
    size?: number;
    current?: number;
    records?: AdminVO[];
    total?: number;
    pages?: number;
  };

  type IPageAppointmentVO = {
    size?: number;
    current?: number;
    records?: AppointmentVO[];
    total?: number;
    pages?: number;
  };

  type IPageBillVO = {
    size?: number;
    current?: number;
    records?: BillVO[];
    total?: number;
    pages?: number;
  };

  type IPageDoctorVO = {
    size?: number;
    current?: number;
    records?: DoctorVO[];
    total?: number;
    pages?: number;
  };

  type IPagePrescriptionVO = {
    size?: number;
    current?: number;
    records?: PrescriptionVO[];
    total?: number;
    pages?: number;
  };

  type IPageScheduleVO = {
    size?: number;
    current?: number;
    records?: ScheduleVO[];
    total?: number;
    pages?: number;
  };

  type IPageUserVO = {
    size?: number;
    current?: number;
    records?: UserVO[];
    total?: number;
    pages?: number;
  };

  type listAppointmentByDoctorParams = {
    /** 开始日期 */
    startDate?: string;
    /** 结束日期 */
    endDate?: string;
  };

  type listAppointmentByUserParams = {
    /** 开始日期 */
    startDate?: string;
    /** 结束日期 */
    endDate?: string;
  };

  type listBillPageParams = {
    /** 当前页 */
    current?: number;
    /** 每页大小 */
    pageSize?: number;
    /** 用户ID */
    userId?: number;
    /** 预约ID */
    appointmentId?: number;
    /** 账单编号 */
    billNo?: string;
    /** 状态 */
    status?: string;
  };

  type listByAppointmentIdParams = {
    /** 预约ID */
    appointmentId: number;
  };

  type listByBillIdParams = {
    /** 账单ID */
    billId: number;
  };

  type listByPrescriptionIdParams = {
    /** 处方ID */
    prescriptionId: number;
  };

  type listByUserId1Params = {
    /** 用户ID */
    userId: number;
  };

  type listByUserId2Params = {
    /** 用户ID */
    userId: number;
  };

  type listByUserIdParams = {
    /** 用户ID */
    userId: number;
  };

  type listDoctorByDepartmentParams = {
    department: string;
  };

  type listMessagesParams = {
    sessionId: string;
    limit?: number;
  };

  type listOnDutyDoctorsParams = {
    /** 排班日期 */
    scheduleDate: string;
    /** 班次类型 */
    shiftType?: string;
  };

  type listPrescriptionByAppointmentParams = {
    /** 预约ID */
    appointmentId: number;
  };

  type listScheduleByDepartmentParams = {
    /** 科室名称 */
    department: string;
    /** 排班日期 */
    scheduleDate: string;
  };

  type listScheduleByDoctorParams = {
    /** 医生ID */
    doctorId: number;
    /** 开始日期 */
    startDate?: string;
    /** 结束日期 */
    endDate?: string;
  };

  type NodeResult = {
    id?: string;
    label?: string;
    name?: string;
    properties?: Record<string, any>;
  };

  type Pagination = {
    page?: number;
    pageSize?: number;
    totalPages?: number;
    hasNext?: boolean;
    hasPrevious?: boolean;
  };

  type PathResult = {
    nodes?: NodeResult[];
    relationships?: RelationResult[];
    weight?: number;
  };

  type PaymentRequest = {
    billId?: number;
    amount?: number;
    paymentType?: string;
    userName?: string;
  };

  type PaymentVO = {
    id?: number;
    paymentNo?: string;
    billId?: number;
    userId?: number;
    userName?: string;
    amount?: number;
    paymentType?: string;
    thirdPartyNo?: string;
    status?: number;
    statusDesc?: string;
    payTime?: string;
    callbackData?: string;
    createTime?: string;
    updateTime?: string;
  };

  type payParams = {
    /** 支付ID */
    paymentId: number;
  };

  type PrescriptionAddRequest = {
    appointmentId?: number;
    diagnosis?: string;
    remark?: string;
    drugs?: PrescriptionDrugItem[];
  };

  type PrescriptionDrugItem = {
    drugCode?: string;
    drugName?: string;
    specification?: string;
    dosage?: string;
    usage?: string;
    frequency?: string;
    duration?: string;
    quantity?: number;
  };

  type PrescriptionItemVO = {
    id?: number;
    prescriptionId?: number;
    drugCode?: string;
    drugName?: string;
    specification?: string;
    dosage?: string;
    usage?: string;
    frequency?: string;
    duration?: string;
    quantity?: number;
    unitPrice?: number;
    totalAmount?: number;
    remark?: string;
    createTime?: string;
  };

  type PrescriptionQueryRequest = {
    id?: number;
    prescriptionNo?: string;
    userId?: number;
    doctorId?: number;
    department?: string;
    status?: number;
    appointmentId?: number;
    current?: number;
    pageSize?: number;
  };

  type PrescriptionStatusUpdateRequest = {
    prescriptionId?: number;
    status?: number;
    remark?: string;
  };

  type PrescriptionVO = {
    id?: number;
    prescriptionNo?: string;
    appointmentId?: number;
    userId?: number;
    userName?: string;
    doctorId?: number;
    doctorName?: string;
    department?: string;
    diagnosis?: string;
    totalAmount?: number;
    status?: number;
    statusText?: string;
    auditTime?: string;
    auditUserId?: number;
    auditRemark?: string;
    dispenseTime?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
    items?: PrescriptionItemVO[];
  };

  type PriceAddRequest = {
    drugId?: number;
    priceType?: string;
    price?: number;
    effectiveDate?: string;
  };

  type QueryResultDTO = {
    queryId?: string;
    query?: string;
    queryType?: string;
    executionTime?: number;
    totalCount?: number;
    nodes?: NodeResult[];
    relations?: RelationResult[];
    paths?: PathResult[];
    records?: Record<string, any>[];
    pagination?: Pagination;
  };

  type RefundRequest = {
    paymentId?: number;
    refundAmount?: number;
    reason?: string;
  };

  type RelationResult = {
    sourceId?: string;
    targetId?: string;
    type?: string;
    properties?: Record<string, any>;
  };

  type ResetPasswordRequest = {
    id?: number;
    newPassword?: string;
  };

  type RoleInfo = {
    department?: string;
    title?: string;
    consultationFee?: number;
    permissions?: string[];
  };

  type ScheduleAddRequest = {
    doctorId?: number;
    doctorName?: string;
    department?: string;
    scheduleDate?: string;
    shiftType?: string;
    maxAppointments?: number;
    description?: string;
  };

  type ScheduleQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    doctorId?: number;
    doctorName?: string;
    department?: string;
    scheduleDate?: string;
    shiftType?: string;
    status?: number;
    onDutyStatus?: number;
    startDate?: string;
    endDate?: string;
  };

  type ScheduleUpdateRequest = {
    id?: number;
    doctorId?: number;
    doctorName?: string;
    department?: string;
    scheduleDate?: string;
    shiftType?: string;
    maxAppointments?: number;
    status?: number;
    description?: string;
  };

  type ScheduleVO = {
    id?: number;
    doctorId?: number;
    doctorName?: string;
    department?: string;
    scheduleDate?: string;
    shiftType?: string;
    shiftName?: string;
    timeRange?: string;
    timeStart?: string;
    timeEnd?: string;
    maxAppointments?: number;
    currentAppointments?: number;
    status?: number;
    onDutyStatus?: number;
    description?: string;
    createTime?: string;
    updateTime?: string;
  };

  type SlotAddRequest = {
    scheduleId?: number;
    timeSlot?: string;
    timeStart?: string;
    timeEnd?: string;
    maxSlots?: number;
  };

  type SlotBatchAddRequest = {
    scheduleId?: number;
    slots?: SlotAddRequest[];
  };

  type SseEmitter = {
    timeout?: number;
  };

  type suggestSymptomsParams = {
    prefix: string;
    limit?: number;
  };

  type SymptomDiagnosisRowVO = {
    symptom?: string;
    disease?: string;
    diseaseCode?: string;
    icdCode?: string;
    icdDescription?: string;
    weight?: number;
  };

  type updateAppointmentStatusParams = {
    /** 预约ID */
    appointmentId: number;
    /** 状态 */
    status: number;
  };

  type updateSlotParams = {
    /** 号源ID */
    id: number;
  };

  type UserIdRequest = {
    id?: number;
  };

  type UserQueryRequest = {
    /** 当前页码 */
    current?: number;
    /** 每页数量 */
    pageSize?: number;
    /** 用户账号（模糊查询） */
    userAccount?: string;
    /** 用户姓名（模糊查询） */
    userName?: string;
    /** 角色：user/admin */
    userRole?: string;
    /** 账号状态：0-禁用，1-正常 */
    userStatus?: number;
  };

  type UserUpdateRequest = {
    /** 用户姓名 */
    userName?: string;
    /** 性别：0-未知，1-男，2-女 */
    gender?: number;
    /** 手机号 */
    phone?: string;
    /** 邮箱 */
    email?: string;
    /** 紧急联系人 */
    emergencyContact?: string;
    /** 紧急联系电话 */
    emergencyPhone?: string;
    /** 用户状态：0-禁用，1-正常 */
    userStatus?: number;
  };

  type UserUpdateWithIdRequest = {
    /** 用户姓名 */
    userName?: string;
    /** 性别：0-未知，1-男，2-女 */
    gender?: number;
    /** 手机号 */
    phone?: string;
    /** 邮箱 */
    email?: string;
    /** 紧急联系人 */
    emergencyContact?: string;
    /** 紧急联系电话 */
    emergencyPhone?: string;
    /** 用户状态：0-禁用，1-正常 */
    userStatus?: number;
    id?: number;
  };

  type UserVO = {
    /** 用户ID */
    id?: number;
    /** 用户账号（已脱敏） */
    userAccount?: string;
    /** 用户昵称 */
    userName?: string;
    /** 用户角色 */
    userRole?: string;
    /** 手机号（已脱敏） */
    phone?: string;
    /** 邮箱（已脱敏） */
    email?: string;
    /** 性别 0-未知 1-男 2-女 */
    gender?: number;
    /** 年龄 */
    age?: number;
    /** 注册时间 */
    createTime?: string;
  };
}
