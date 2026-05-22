<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, registerAdmin } from '@/services/medical/authController'
import AuthLayout from '@/components/layout/AuthLayout.vue'

const router = useRouter()

const registerForm = reactive({
  userRole: 'user',
  userAccount: '',
  userPassword: '',
  confirmPassword: '',
  userName: '',
  phone: '',
  licenseNo: '',
  department: '',
  title: '',
  consultationFee: 50
})

const loading = ref(false)

const departments = ['内科', '外科', '儿科', '妇产科', '眼科', '口腔科', '皮肤科', '骨科']
const titleOptions = ['主任医师', '副主任医师', '主治医师', '住院医师', '医士']

const handleRegister = async () => {
  if (!registerForm.userAccount || !registerForm.userPassword || !registerForm.userName) {
    ElMessage.warning('请填写完整')
    return
  }
  if (registerForm.userPassword !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (registerForm.userRole === 'doctor') {
    if (!registerForm.licenseNo) {
      ElMessage.warning('请填写执业证书编号')
      return
    }
    if (!registerForm.department) {
      ElMessage.warning('请选择科室')
      return
    }
    if (!registerForm.title) {
      ElMessage.warning('请选择职称')
      return
    }
  }
  if (registerForm.userRole === 'admin' && registerForm.userPassword.length < 8) {
    ElMessage.warning('管理员密码长度不能少于8位')
    return
  }

  loading.value = true
  try {
    const body: API.AuthRegisterRequest = {
      userAccount: registerForm.userAccount,
      userPassword: registerForm.userPassword,
      checkPassword: registerForm.confirmPassword,
      userName: registerForm.userName,
      userRole: registerForm.userRole,
      phone: registerForm.phone || undefined
    }
    if (registerForm.userRole === 'doctor') {
      body.licenseNo = registerForm.licenseNo
      body.department = registerForm.department
      body.title = registerForm.title
      body.consultationFee = registerForm.consultationFee
    }
    if (registerForm.userRole === 'admin') {
      await registerAdmin(body)
      ElMessage.success('临时管理员注册成功，登录后需由正式管理员升级为正式账号')
    } else {
      await register(body)
      ElMessage.success('注册成功，请登录')
    }
    router.push('/auth/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <div class="auth-form register-form">
      <header class="form-header">
        <h2>账号注册</h2>
        <p class="form-desc">请填写真实信息完成注册，医护人员需补充执业信息</p>
      </header>

      <el-form :model="registerForm" label-width="100px" label-position="right" class="auth-el-form">
        <el-form-item label="角色">
          <el-radio-group v-model="registerForm.userRole">
            <el-radio value="user">患者</el-radio>
            <el-radio value="doctor">医生</el-radio>
            <el-radio value="pharmacist">药师</el-radio>
            <el-radio value="admin">
              管理员
              <el-tag size="small" type="warning" class="role-tag-temp">临时</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="registerForm.userName" placeholder="请输入姓名" autocomplete="name" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="registerForm.userAccount" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="registerForm.userPassword"
            type="password"
            placeholder="请输入密码（至少8位）"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item v-if="registerForm.userRole !== 'admin'" label="手机号">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号（选填）" autocomplete="tel" />
        </el-form-item>
        <template v-if="registerForm.userRole === 'admin'">
          <el-alert
            title="通过公开注册的管理员账号默认标记为「临时管理员」，权限与正式管理员相同。"
            type="warning"
            :closable="false"
            show-icon
            class="role-alert"
          />
        </template>
        <template v-if="registerForm.userRole === 'doctor'">
          <el-form-item label="执业证书">
            <el-input v-model="registerForm.licenseNo" placeholder="请输入执业证书编号" />
          </el-form-item>
          <el-form-item label="职称">
            <el-select v-model="registerForm.title" placeholder="请选择职称" style="width: 100%;">
              <el-option v-for="title in titleOptions" :key="title" :label="title" :value="title" />
            </el-select>
          </el-form-item>
          <el-form-item label="科室">
            <el-select v-model="registerForm.department" placeholder="请选择科室" style="width: 100%;">
              <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
            </el-select>
          </el-form-item>
        </template>
        <template v-if="registerForm.userRole === 'pharmacist'">
          <el-alert
            title="药师注册后可进入药师工作台，处理处方发药相关任务。"
            type="info"
            :closable="false"
            show-icon
            class="role-alert"
          />
        </template>
        <el-form-item class="submit-item">
          <el-button
            type="primary"
            :loading="loading"
            class="submit-btn"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-switch-link">
        <span>已有账号？</span>
        <router-link to="/auth/login" class="link-primary">立即登录</router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<style scoped>
.form-header {
  margin-bottom: 24px;
  text-align: center;
}

.form-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #1e3a4a;
}

.form-desc {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.role-tag-temp {
  margin-left: 6px;
  vertical-align: middle;
}

.role-alert {
  margin-bottom: 16px;
}

.auth-el-form :deep(.el-form-item__label) {
  color: #475569;
  font-weight: 500;
}

.submit-item {
  margin-bottom: 0;
}

.submit-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.submit-btn {
  width: 100%;
  min-height: 44px;
  font-size: 15px;
  letter-spacing: 0.05em;
}

.auth-switch-link {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

.link-primary {
  color: #0d9488;
  text-decoration: none;
  margin-left: 4px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.link-primary:hover {
  color: #0f766e;
  text-decoration: underline;
}

.register-form :deep(.el-radio-group) {
  flex-wrap: wrap;
  gap: 4px 0;
}
</style>
