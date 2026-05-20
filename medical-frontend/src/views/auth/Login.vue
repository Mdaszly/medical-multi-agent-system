<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getHomeRoute } from '@/router'
import { post } from '@/request'
import AuthLayout from '@/components/layout/AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

const loginForm = reactive({
  userAccount: '',
  userPassword: '',
  userRole: 'user'
})

const loading = ref(false)

// 测试账号数据
const testAccounts = [
  {
    role: 'user',
    label: '患者账号',
    userAccount: 'patient001',
    userPassword: 'Patient@123',
    color: '#10b981'
  },
  {
    role: 'doctor',
    label: '医生账号',
    userAccount: 'doctor001',
    userPassword: 'Doctor@123',
    color: '#3b82f6'
  },
  {
    role: 'pharmacist',
    label: '药师账号',
    userAccount: 'pharmacist001',
    userPassword: 'Pharmacist@123',
    color: '#f59e0b'
  },
  {
    role: 'admin',
    label: '管理员账号',
    userAccount: 'admin_test',
    userPassword: 'AdminTest@123456',
    color: '#8b5cf6'
  }
]

// 一键填入测试账号
const fillTestAccount = (account: typeof testAccounts[0]) => {
  loginForm.userRole = account.role
  loginForm.userAccount = account.userAccount
  loginForm.userPassword = account.userPassword
  ElMessage.success(`已填入${account.label}`)
}

const handleLogin = async () => {
  if (!loginForm.userAccount || !loginForm.userPassword) {
    ElMessage.warning('请填写完整')
    return
  }

  loading.value = true
  try {
    const response = await post('/api/auth/login', {
      userAccount: loginForm.userAccount,
      password: loginForm.userPassword
    })

    if (response.data) {
      const userRole = response.data.userRole || loginForm.userRole
      authStore.setUserInfo({
        ...response.data,
        userRole
      })

      ElMessage.success('登录成功！')
      const roleHome = getHomeRoute(userRole)
      router.push(roleHome)
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <div class="login-form">
      <h2>登录</h2>

      <!-- 测试账号快捷选择 -->
      <div class="test-accounts">
        <div class="test-accounts-title">
          <span>🎯 测试账号（一键填入）</span>
        </div>
        <div class="test-accounts-list">
          <el-button
            v-for="account in testAccounts"
            :key="account.role"
            size="small"
            :style="{ borderColor: account.color, color: account.color }"
            @click="fillTestAccount(account)"
          >
            {{ account.label }}
          </el-button>
        </div>
      </div>

      <el-divider />

      <el-form :model="loginForm" label-width="80px">
        <el-form-item label="角色">
          <el-radio-group v-model="loginForm.userRole">
            <el-radio value="user">患者</el-radio>
            <el-radio value="doctor">医生</el-radio>
            <el-radio value="pharmacist">药师</el-radio>
            <el-radio value="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="loginForm.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.userPassword" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%;">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="register-link">
        <span>还没有账号？</span>
        <router-link to="/auth/register">立即注册</router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<style scoped>
.login-form h2 {
  margin-bottom: 30px;
  color: #1f2937;
}

.test-accounts {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.test-accounts-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.test-accounts-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.test-accounts-list .el-button {
  transition: all 0.2s;
}

.test-accounts-list .el-button:hover {
  transform: translateY(-2px);
}

.register-link {
  margin-top: 20px;
  color: #6b7280;
}

.register-link a {
  color: #06b6d4;
  text-decoration: none;
  margin-left: 5px;
}
</style>
