# 医疗多Agent系统 - 完整接口测试方案

## 📋 文档说明

| 项目信息   | 说明                             |
| ------ | ------------------------------ |
| 项目名称   | 医疗多Agent系统                     |
| 测试方案版本 | v1.0                           |
| 创建日期   | 2026-05-17                     |
| 测试工具   | Apifox                         |
| 测试环境   | 开发环境 (<http://localhost:8080>) |

***

## 🔐 测试账号信息

基于提供的登录信息，以下是各角色测试账号：

| 角色  | 账号              | 密码              | 备注           |
| --- | --------------- | --------------- | ------------ |
| 管理员 | `admin_test`    | `Admin123456`   | 系统管理员，拥有全部权限 |
| 医生  | `doctor001`     | `Doctor123`     | 医生角色，可开处方、诊疗 |
| 药师  | `pharmacist001` | `Pharmacist123` | 药师角色，可发药     |
| 患者  | `patient001`    | `Patient123`    | 患者角色，可挂号、支付  |

***

## 📊 完整业务流程图

```
患者登录
   ↓
查询号源 → 选择排班 → 预约挂号
   ↓
预约签到
   ↓
医生登录
   ↓
查询预约患者 → 创建诊疗处方
   ↓
创建费用项
   ↓
生成账单
   ↓
患者登录
   ↓
查询账单 → 创建支付记录 → 发起支付
   ↓
支付成功 → 更新账单状态
   ↓
药师登录
   ↓
查询待发药处方 → 发药
   ↓
完成
```

***

## 📝 测试用例详细说明

***

### 一、登录认证模块

#### 1.1 管理员登录

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 管理员登录                                  |
| **请求方法**  | `POST`                                 |
| **请求URL** | `http://localhost:8080/api/auth/login` |
| **请求头**   | `Content-Type: application/json`       |

**请求体 (Body - JSON):**

```json
{
  "userAccount": "admin_test",
  "password": "Admin123456"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "userId": 1,
    "userAccount": "admin_test",
    "userName": "管理员",
    "userRole": "admin",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "message": "登录成功"
}
```

**后置操作**: 将响应中的 `token` 保存到环境变量 `{{adminToken}}`

***

#### 1.2 医生登录

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 医生登录                                   |
| **请求方法**  | `POST`                                 |
| **请求URL** | `http://localhost:8080/api/auth/login` |
| **请求头**   | `Content-Type: application/json`       |

**请求体 (Body - JSON):**

```json
{
  "userAccount": "doctor001",
  "password": "Doctor123"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "userId": 2,
    "userAccount": "doctor001",
    "userName": "张医生",
    "userRole": "doctor",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "message": "登录成功"
}
```

**后置操作**: 将响应中的 `token` 保存到环境变量 `{{doctorToken}}`

***

#### 1.3 药师登录

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 药师登录                                   |
| **请求方法**  | `POST`                                 |
| **请求URL** | `http://localhost:8080/api/auth/login` |
| **请求头**   | `Content-Type: application/json`       |

**请求体 (Body - JSON):**

```json
{
  "userAccount": "pharmacist001",
  "password": "Pharmacist123"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "userId": 3,
    "userAccount": "pharmacist001",
    "userName": "李药师",
    "userRole": "pharmacist",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "message": "登录成功"
}
```

**后置操作**: 将响应中的 `token` 保存到环境变量 `{{pharmacistToken}}`

***

#### 1.4 患者登录

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 患者登录                                   |
| **请求方法**  | `POST`                                 |
| **请求URL** | `http://localhost:8080/api/auth/login` |
| **请求头**   | `Content-Type: application/json`       |

**请求体 (Body - JSON):**

```json
{
  "userAccount": "patient001",
  "password": "Patient123"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "userId": 4,
    "userAccount": "patient001",
    "userName": "王患者",
    "userRole": "user",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "message": "登录成功"
}
```

**后置操作**: 将响应中的 `token` 保存到环境变量 `{{patientToken}}`

***

#### 1.5 获取当前登录用户信息

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 获取当前用户                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/auth/current`                               |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "userId": 4,
    "userAccount": "patient001",
    "userName": "王患者",
    "userRole": "user"
  },
  "message": "success"
}
```

***

#### 1.6 边界测试 - 账号或密码错误

**Apifox 测试样例**

| 项目        | 内容                                     |
| --------- | -------------------------------------- |
| **接口名称**  | 登录失败测试                                 |
| **请求方法**  | `POST`                                 |
| **请求URL** | `http://localhost:8080/api/auth/login` |
| **请求头**   | `Content-Type: application/json`       |

**请求体 (Body - JSON):**

```json
{
  "userAccount": "patient001",
  "password": "WrongPassword"
}
```

**预期响应:**

```json
{
  "code": 40001,
  "data": null,
  "message": "账号或密码错误"
}
```

***

### 二、挂号签到模块

#### 2.1 获取医生排班号源列表（前置条件：先查询排班ID）

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 获取号源列表                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/appointment/slots`                          |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |
| **查询参数**  | `scheduleId=1`                                                         |

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "slotId": 1,
      "timeSlot": "08:00-08:30",
      "status": "AVAILABLE",
      "quota": 10,
      "booked": 3
    },
    {
      "slotId": 2,
      "timeSlot": "08:30-09:00",
      "status": "AVAILABLE",
      "quota": 10,
      "booked": 5
    }
  ],
  "message": "success"
}
```

**后置操作**: 将可用的 `slotId` 和 `scheduleId` 保存到环境变量

***

#### 2.2 创建预约挂号

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 创建预约                                                                   |
| **请求方法**  | `POST`                                                                 |
| **请求URL** | `http://localhost:8080/api/appointment/create`                         |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**请求体 (Body - JSON):**

```json
{
  "scheduleId": 1,
  "timeSlot": "08:00-08:30",
  "remark": "复诊检查"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "appointmentId": 1001,
    "appointmentNo": "APT20260517120000001",
    "userId": 4,
    "userName": "王患者",
    "doctorId": 2,
    "doctorName": "张医生",
    "scheduleId": 1,
    "timeSlot": "08:00-08:30",
    "status": "PENDING",
    "createTime": "2026-05-17T10:30:00"
  },
  "message": "success"
}
```

**后置操作**: 将响应中的 `appointmentId` 保存到环境变量 `{{appointmentId}}`

***

#### 2.3 查询用户预约列表

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 查询用户预约                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/appointment/list/user`                      |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "appointmentId": 1001,
      "appointmentNo": "APT20260517120000001",
      "doctorName": "张医生",
      "timeSlot": "08:00-08:30",
      "status": "PENDING"
    }
  ],
  "message": "success"
}
```

***

#### 2.4 预约签到

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 预约签到                                                                   |
| **请求方法**  | `POST`                                                                 |
| **请求URL** | `http://localhost:8080/api/appointment/checkin`                        |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |
| **查询参数**  | `appointmentId={{appointmentId}}`                                      |

**预期响应:**

```json
{
  "code": 0,
  "data": null,
  "message": "success"
}
```

***

#### 2.5 医生查询预约患者列表

**Apifox 测试样例**

| 项目        | 内容                                                                    |
| --------- | --------------------------------------------------------------------- |
| **接口名称**  | 查询医生预约                                                                |
| **请求方法**  | `GET`                                                                 |
| **请求URL** | `http://localhost:8080/api/appointment/list/doctor`                   |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{doctorToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "appointmentId": 1001,
      "appointmentNo": "APT20260517120000001",
      "userName": "王患者",
      "timeSlot": "08:00-08:30",
      "status": "CHECKED_IN"
    }
  ],
  "message": "success"
}
```

***

#### 2.6 边界测试 - 取消预约

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 取消预约                                                                   |
| **请求方法**  | `POST`                                                                 |
| **请求URL** | `http://localhost:8080/api/appointment/cancel`                         |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**请求体 (Body - JSON):**

```json
{
  "appointmentId": 1002,
  "reason": "临时有事"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": null,
  "message": "success"
}
```

***

### 三、处方药品模块

#### 3.1 查询药品列表（医生登录）

**Apifox 测试样例**

| 项目        | 内容                                                                    |
| --------- | --------------------------------------------------------------------- |
| **接口名称**  | 药品列表                                                                  |
| **请求方法**  | `POST`                                                                |
| **请求URL** | `http://localhost:8080/api/drug/list`                                 |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{doctorToken}}` |

**请求体 (Body - JSON):**

```json
{
  "keyword": "",
  "category": ""
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "drugId": 1,
      "drugCode": "DRG001",
      "drugName": "阿莫西林胶囊",
      "specification": "0.5g*24粒",
      "category": "ANTIBIOTIC",
      "currentPrice": 25.50
    },
    {
      "drugId": 2,
      "drugCode": "DRG002",
      "drugName": "感冒灵颗粒",
      "specification": "10g*9袋",
      "category": "COLD",
      "currentPrice": 18.00
    }
  ],
  "message": "success"
}
```

**后置操作**: 选择药品并保存药品信息

***

#### 3.2 医生创建处方

**Apifox 测试样例**

| 项目        | 内容                                                                    |
| --------- | --------------------------------------------------------------------- |
| **接口名称**  | 创建处方                                                                  |
| **请求方法**  | `POST`                                                                |
| **请求URL** | `http://localhost:8080/api/prescription/create`                       |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{doctorToken}}` |

**请求体 (Body - JSON):**

```json
{
  "appointmentId": {{appointmentId}},
  "diagnosis": "上呼吸道感染",
  "remark": "饭后服用",
  "drugs": [
    {
      "drugCode": "DRG001",
      "drugName": "阿莫西林胶囊",
      "specification": "0.5g*24粒",
      "dosage": "2粒",
      "usage": "口服",
      "frequency": "每日3次",
      "duration": "3天",
      "quantity": 2
    },
    {
      "drugCode": "DRG002",
      "drugName": "感冒灵颗粒",
      "specification": "10g*9袋",
      "dosage": "1袋",
      "usage": "冲服",
      "frequency": "每日3次",
      "duration": "3天",
      "quantity": 1
    }
  ]
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "prescriptionId": 5001,
    "prescriptionNo": "RCP20260517123456789",
    "appointmentId": 1001,
    "doctorId": 2,
    "doctorName": "张医生",
    "userId": 4,
    "userName": "王患者",
    "diagnosis": "上呼吸道感染",
    "status": "AUDITED",
    "createTime": "2026-05-17T11:00:00",
    "drugs": [
      {
        "drugCode": "DRG001",
        "drugName": "阿莫西林胶囊",
        "quantity": 2
      },
      {
        "drugCode": "DRG002",
        "drugName": "感冒灵颗粒",
        "quantity": 1
      }
    ]
  },
  "message": "success"
}
```

**后置操作**: 将 `prescriptionId` 保存到环境变量 `{{prescriptionId}}`

***

#### 3.3 查询处方详情

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 获取处方详情                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/prescription/get`                           |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |
| **查询参数**  | `id={{prescriptionId}}`                                                |

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "prescriptionId": 5001,
    "prescriptionNo": "RCP20260517123456789",
    "diagnosis": "上呼吸道感染",
    "status": "AUDITED",
    "drugs": [...]
  },
  "message": "success"
}
```

***

#### 3.4 查询用户处方列表

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 查询用户处方                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/prescription/list/user`                     |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "prescriptionId": 5001,
      "prescriptionNo": "RCP20260517123456789",
      "doctorName": "张医生",
      "diagnosis": "上呼吸道感染",
      "status": "AUDITED",
      "createTime": "2026-05-17T11:00:00"
    }
  ],
  "message": "success"
}
```

***

#### 3.5 药师查询待发药处方

**Apifox 测试样例**

| 项目        | 内容                                                                        |
| --------- | ------------------------------------------------------------------------- |
| **接口名称**  | 查询待发药处方                                                                   |
| **请求方法**  | `GET`                                                                     |
| **请求URL** | `http://localhost:8080/api/prescription/list/pending-dispense`            |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{pharmacistToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "prescriptionId": 5001,
      "prescriptionNo": "RCP20260517123456789",
      "userName": "王患者",
      "doctorName": "张医生",
      "diagnosis": "上呼吸道感染",
      "status": "AUDITED"
    }
  ],
  "message": "success"
}
```

***

#### 3.6 药房发药

**Apifox 测试样例**

| 项目        | 内容                                                                        |
| --------- | ------------------------------------------------------------------------- |
| **接口名称**  | 发药                                                                        |
| **请求方法**  | `POST`                                                                    |
| **请求URL** | `http://localhost:8080/api/prescription/dispense`                         |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{pharmacistToken}}` |
| **查询参数**  | `prescriptionId={{prescriptionId}}`                                       |

**预期响应:**

```json
{
  "code": 0,
  "data": null,
  "message": "success"
}
```

**验证点**: 再次查询处方，状态应为 "DISPENSED"

***

### 四、费用账单模块

#### 4.1 生成账单（基于预约）

**Apifox 测试样例**

| 项目        | 内容                                                                      |
| --------- | ----------------------------------------------------------------------- |
| **接口名称**  | 根据预约生成账单                                                                |
| **请求方法**  | `POST`                                                                  |
| **请求URL** | `http://localhost:8080/api/bill/generate/appointment/{{appointmentId}}` |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{doctorToken}}`   |

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "billId": 3001,
    "billNo": "BIL20260517123456789",
    "userId": 4,
    "userName": "王患者",
    "appointmentId": 1001,
    "totalAmount": 100.00,
    "discountAmount": 0.00,
    "insuranceAmount": 30.00,
    "selfPayAmount": 70.00,
    "paidAmount": 0.00,
    "status": "UNPAID",
    "createTime": "2026-05-17T11:30:00"
  },
  "message": "success"
}
```

**后置操作**: 将 `billId` 保存到环境变量 `{{billId}}`

***

#### 4.2 查询账单详情

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 查询账单详情                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/bill/{{billId}}`                            |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "billId": 3001,
    "billNo": "BIL20260517123456789",
    "totalAmount": 100.00,
    "discountAmount": 0.00,
    "insuranceAmount": 30.00,
    "selfPayAmount": 70.00,
    "paidAmount": 0.00,
    "status": "UNPAID"
  },
  "message": "success"
}
```

***

#### 4.3 查询用户账单列表

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 查询用户账单                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/bill/list/user/{{userId}}`                  |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "billId": 3001,
      "billNo": "BIL20260517123456789",
      "totalAmount": 100.00,
      "selfPayAmount": 70.00,
      "status": "UNPAID",
      "createTime": "2026-05-17T11:30:00"
    }
  ],
  "message": "success"
}
```

***

### 五、支付模块

#### 5.1 创建支付记录

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 创建支付记录                                                                 |
| **请求方法**  | `POST`                                                                 |
| **请求URL** | `http://localhost:8080/api/payment/create`                             |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**请求体 (Body - JSON):**

```json
{
  "billId": {{billId}},
  "amount": 70.00,
  "paymentType": "WECHAT",
  "userName": "王患者"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "paymentId": 4001,
    "paymentNo": "PAY20260517123456789",
    "billId": 3001,
    "userId": 4,
    "userName": "王患者",
    "amount": 70.00,
    "paymentType": "WECHAT",
    "status": 0,
    "statusDesc": "待支付",
    "createTime": "2026-05-17T12:00:00"
  },
  "message": "success"
}
```

**后置操作**: 将 `paymentId` 保存到环境变量 `{{paymentId}}`

***

#### 5.2 发起支付（模拟）

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 发起支付                                                                   |
| **请求方法**  | `POST`                                                                 |
| **请求URL** | `http://localhost:8080/api/payment/pay/{{paymentId}}`                  |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "paymentId": 4001,
    "paymentNo": "PAY20260517123456789",
    "status": 1,
    "statusDesc": "已支付",
    "payTime": "2026-05-17T12:00:30",
    "thirdPartyNo": "WX1712345678901234"
  },
  "message": "success"
}
```

**验证点**:

1. 支付状态应为 1 (已支付)
2. 账单状态应更新为 "PAID"
3. 费用项应标记为已结算

***

#### 5.3 查询支付记录

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 查询支付记录                                                                 |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/payment/{{paymentId}}`                      |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "paymentId": 4001,
    "paymentNo": "PAY20260517123456789",
    "amount": 70.00,
    "paymentType": "WECHAT",
    "status": 1,
    "statusDesc": "已支付",
    "payTime": "2026-05-17T12:00:30"
  },
  "message": "success"
}
```

***

#### 5.4 查询账单支付记录

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 查询账单支付记录                                                               |
| **请求方法**  | `GET`                                                                  |
| **请求URL** | `http://localhost:8080/api/payment/list/bill/{{billId}}`               |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**预期响应:**

```json
{
  "code": 0,
  "data": [
    {
      "paymentId": 4001,
      "paymentNo": "PAY20260517123456789",
      "amount": 70.00,
      "status": 1,
      "statusDesc": "已支付"
    }
  ],
  "message": "success"
}
```

***

#### 5.5 边界测试 - 退款

**Apifox 测试样例**

| 项目        | 内容                                                                     |
| --------- | ---------------------------------------------------------------------- |
| **接口名称**  | 发起退款                                                                   |
| **请求方法**  | `POST`                                                                 |
| **请求URL** | `http://localhost:8080/api/payment/refund`                             |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{patientToken}}` |

**请求体 (Body - JSON):**

```json
{
  "paymentId": {{paymentId}},
  "refundAmount": 70.00,
  "reason": "重复支付"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "paymentId": 4001,
    "status": 3,
    "statusDesc": "已退款"
  },
  "message": "success"
}
```

***

#### 5.6 账单导出测试

**Apifox 测试样例**

| 项目        | 内容                                                                   |
| --------- | -------------------------------------------------------------------- |
| **接口名称**  | 导出账单                                                                 |
| **请求方法**  | `GET`                                                                |
| **请求URL** | `http://localhost:8080/api/bill/export`                              |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{adminToken}}` |
| **查询参数**  | `userId=4&status=PAID`                                               |

**预期响应**: 返回CSV格式文件，包含账单数据

***

### 六、管理员功能测试

#### 6.1 分页查询预约列表

**Apifox 测试样例**

| 项目        | 内容                                                                   |
| --------- | -------------------------------------------------------------------- |
| **接口名称**  | 分页查询预约                                                               |
| **请求方法**  | `POST`                                                               |
| **请求URL** | `http://localhost:8080/api/appointment/list/page`                    |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{adminToken}}` |

**请求体 (Body - JSON):**

```json
{
  "current": 1,
  "pageSize": 10,
  "userId": null,
  "status": null
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1
  },
  "message": "success"
}
```

***

#### 6.2 新增药品

**Apifox 测试样例**

| 项目        | 内容                                                                   |
| --------- | -------------------------------------------------------------------- |
| **接口名称**  | 新增药品                                                                 |
| **请求方法**  | `POST`                                                               |
| **请求URL** | `http://localhost:8080/api/drug/add`                                 |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{adminToken}}` |

**请求体 (Body - JSON):**

```json
{
  "drugCode": "DRG003",
  "drugName": "布洛芬缓释胶囊",
  "specification": "0.3g*20粒",
  "category": "ANALGESIC",
  "manufacturer": "中美史克",
  "unit": "盒"
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "drugId": 3,
    "drugCode": "DRG003",
    "drugName": "布洛芬缓释胶囊"
  },
  "message": "success"
}
```

***

#### 6.3 分页查询处方

**Apifox 测试样例**

| 项目        | 内容                                                                   |
| --------- | -------------------------------------------------------------------- |
| **接口名称**  | 分页查询处方                                                               |
| **请求方法**  | `POST`                                                               |
| **请求URL** | `http://localhost:8080/api/prescription/list/page`                   |
| **请求头**   | `Content-Type: application/jsonAuthorization: Bearer {{adminToken}}` |

**请求体 (Body - JSON):**

```json
{
  "current": 1,
  "pageSize": 10,
  "userId": null,
  "status": null
}
```

**预期响应:**

```json
{
  "code": 0,
  "data": {
    "records": [...],
    "total": 50,
    "size": 10,
    "current": 1
  },
  "message": "success"
}
```

***

## 📊 测试检查清单

### 功能测试

| 测试项     | 测试状态 | 备注              |
| ------- | ---- | --------------- |
| 各角色登录功能 | ☐    | 需验证token获取和权限控制 |
| 预约挂号流程  | ☐    | 从查询号源到签到完成      |
| 处方开具流程  | ☐    | 医生开处方、药师发药      |
| 费用计算    | ☐    | 验证金额计算准确性       |
| 账单生成    | ☐    | 验证账单数据完整性       |
| 支付流程    | ☐    | 创建支付、支付、退款      |
| 数据一致性   | ☐    | 支付后账单、费用项状态更新   |

### 异常测试

| 测试项        | 测试状态 | 备注        |
| ---------- | ---- | --------- |
| 账号密码错误     | ☐    | 登录失败场景    |
| 重复支付同一账单   | ☐    | 应阻止或返回错误  |
| 非待支付状态支付   | ☐    | 应返回业务错误   |
| 退款金额超过支付金额 | ☐    | 应返回业务错误   |
| 权限越界操作     | ☐    | 如患者操作医生功能 |

### 性能测试

| 测试项            | 测试状态 | 备注   |
| -------------- | ---- | ---- |
| 接口响应时间 < 500ms | ☐    | 常规接口 |
| 并发登录测试         | ☐    | 10并发 |
| 大数据量查询         | ☐    | 分页查询 |

***

## 🔧 Apifox 环境配置建议

### 环境变量设置

在 Apifox 中创建以下环境变量：

| 变量名               | 示例值                     | 说明                |
| ----------------- | ----------------------- | ----------------- |
| `baseUrl`         | `http://localhost:8080` | 基础URL             |
| `adminToken`      | `eyJhbGci...`           | 管理员token（登录后自动获取） |
| `doctorToken`     | `eyJhbGci...`           | 医生token（登录后自动获取）  |
| `pharmacistToken` | `eyJhbGci...`           | 药师token（登录后自动获取）  |
| `patientToken`    | `eyJhbGci...`           | 患者token（登录后自动获取）  |
| `appointmentId`   | `1001`                  | 预约ID（动态获取）        |
| `prescriptionId`  | `5001`                  | 处方ID（动态获取）        |
| `billId`          | `3001`                  | 账单ID（动态获取）        |
| `paymentId`       | `4001`                  | 支付ID（动态获取）        |
| `userId`          | `4`                     | 用户ID（登录后获取）       |

***

## 📝 测试数据准备建议

### 1. 测试数据准备脚本

建议在执行测试前，先准备以下测试数据：

```sql
-- 插入测试排班
INSERT INTO schedule (doctor_id, schedule_date, status) 
VALUES (2, '2026-05-18', 'ACTIVE');

-- 插入测试号源
INSERT INTO appointment_slot (schedule_id, time_slot, quota, booked, status)
VALUES 
(1, '08:00-08:30', 10, 0, 'AVAILABLE'),
(1, '08:30-09:00', 10, 0, 'AVAILABLE');

-- 插入测试药品
INSERT INTO drug (drug_code, drug_name, specification, category, status)
VALUES 
('DRG001', '阿莫西林胶囊', '0.5g*24粒', 'ANTIBIOTIC', 'ACTIVE'),
('DRG002', '感冒灵颗粒', '10g*9袋', 'COLD', 'ACTIVE');

-- 插入药品价格
INSERT INTO drug_price (drug_id, price_type, price, effective_date, status)
VALUES 
(1, 'RETAIL', 25.50, '2026-01-01', 'ACTIVE'),
(2, 'RETAIL', 18.00, '2026-01-01', 'ACTIVE');
```

***

## 🎯 完整测试执行顺序

### 阶段一：基础数据准备

1. ✅ 管理员登录
2. ✅ 确认测试药品数据存在
3. ✅ 确认医生排班数据存在

### 阶段二：患者挂号流程

1. ✅ 患者登录
2. ✅ 查询排班号源
3. ✅ 创建预约挂号
4. ✅ 预约签到

### 阶段三：医生诊疗流程

1. ✅ 医生登录
2. ✅ 查询预约患者
3. ✅ 创建处方（含药品）

### 阶段四：账单支付流程

1. ✅ 生成账单
2. ✅ 查询账单详情
3. ✅ 创建支付记录
4. ✅ 发起支付
5. ✅ 验证账单状态更新

### 阶段五：药房发药流程

1. ✅ 药师登录
2. ✅ 查询待发药处方
3. ✅ 执行发药操作
4. ✅ 验证处方状态更新

### 阶段六：异常测试

1. ✅ 登录失败测试
2. ✅ 重复支付测试
3. ✅ 退款测试
4. ✅ 权限越界测试

***

## 📞 注意事项

1. **Token 管理**: 所有需要认证的接口都需要在请求头中携带 `Authorization: Bearer {{token}}`
2. **环境变量**: 测试过程中注意保存动态生成的ID到环境变量
3. **测试顺序**: 严格按照业务流程顺序执行测试用例
4. **数据清理**: 测试完成后，建议清理测试数据或使用独立测试数据库
5. **日志查看**: 遇到问题时，查看后端日志排查问题
6. **接口文档**: 详细的接口文档可通过 Swagger 查看 `http://localhost:8080/swagger-ui.html`

***

**文档版本**: v1.0\
**最后更新**: 2026-05-17\
**维护人员**: 测试团队
