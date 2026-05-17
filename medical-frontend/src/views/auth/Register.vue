<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/layout/AuthLayout.vue'

const router = useRouter()

const registerForm = reactive({
  userRole: 'user',
  userAccount: '',
  userPassword: '',
  confirmPassword: '',
  userName: '',
  doctorNo: '',
  department: ''
})

const loading = ref(false)

const departments = ['内科', '外科', '儿科', '妇产科', '眼科', '口腔科', '皮肤科', '骨科']

const handleRegister = () => {
  if (!registerForm.userAccount || !registerForm.userPassword || !registerForm.userName) {
    ElMessage.warning('请填写完整')
    return
  }
  if (registerForm.userPassword !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (registerForm.userRole === 'doctor' && (!registerForm.doctorNo || !registerForm.department)) {
    ElMessage.warning('请填写医生相关信息')
    return
  }
  
  loading.value = true
  setTimeout(() => {
    ElMessage.success('注册成功，请登录！')
    router.push('/auth/login')
    loading.value = false
  }, 500)
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
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="registerForm.userName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="registerForm.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.userPassword" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item v-if="registerForm.userRole === 'doctor'" label="医生编号">
          <el-input v-model="registerForm.doctorNo" placeholder="请输入医生编号" />
        </el-form-item>
        <el-form-item v-if="registerForm.userRole === 'doctor'" label="科室">
          <el-select v-model="registerForm.department" placeholder="请选择科室" style="width: 100%;">
            <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </el-form-item>
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
