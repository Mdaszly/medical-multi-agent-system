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
      ElMessage.success('临时管理员注册成功！登录后需由正式管理员升级为正式账号。')
    } else {
      await register(body)
      ElMessage.success('注册成功，请登录！')
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
    <div class="register-form">
      <h2>注册</h2>
      <el-form :model="registerForm" label-width="100px">
        <el-form-item label="角色">
          <el-radio-group v-model="registerForm.userRole">
            <el-radio value="user">患者</el-radio>
            <el-radio value="doctor">医生</el-radio>
            <el-radio value="pharmacist">药师</el-radio>
            <el-radio value="admin">
              管理员
              <el-tag size="small" type="warning" style="margin-left: 6px;">临时</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="registerForm.userName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="registerForm.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.userPassword" type="password" placeholder="请输入密码（至少8位）" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item v-if="registerForm.userRole !== 'admin'" label="手机号">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号（选填）" />
        </el-form-item>
        <template v-if="registerForm.userRole === 'admin'">
          <el-alert
            title="提示：通过公开注册的管理员账号默认标记为'临时管理员'，权限与正式管理员相同。"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 16px;"
          />
        </template>
        <template v-if="registerForm.userRole === 'doctor'">
          <el-form-item label="执业证书编号">
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
            title="提示：药师注册后可进入药师工作台处理处方发药任务。"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 16px;"
          />
        </template>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRegister" style="width: 100%;">注册</el-button>
        </el-form-item>
      </el-form>
      <div class="login-link">
        <span>已有账号？</span>
        <router-link to="/auth/login">立即登录</router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<style scoped>
.register-form h2 {
  margin-bottom: 30px;
  color: #1f2937;
}

.login-link {
  margin-top: 20px;
  color: #6b7280;
}

.login-link a {
  color: #06b6d4;
  text-decoration: none;
  margin-left: 5px;
}
</style>
