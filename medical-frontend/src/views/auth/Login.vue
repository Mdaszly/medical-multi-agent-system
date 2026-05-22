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

const testAccounts = [
  {
    role: 'user',
    label: '患者',
    userAccount: 'patient001',
    userPassword: 'Patient@123',
    color: '#059669'
  },
  {
    role: 'doctor',
    label: '医生',
    userAccount: 'doctor001',
    userPassword: 'Doctor@123',
    color: '#2563eb'
  },
  {
    role: 'pharmacist',
    label: '药师',
    userAccount: 'pharmacist001',
    userPassword: 'Pharmacist@123',
    color: '#d97706'
  },
  {
    role: 'admin',
    label: '管理员',
    userAccount: 'admin_test',
    userPassword: 'AdminTest@123456',
    color: '#7c3aed'
  }
]

const fillTestAccount = (account: typeof testAccounts[0]) => {
  loginForm.userRole = account.role
  loginForm.userAccount = account.userAccount
  loginForm.userPassword = account.userPassword
  ElMessage.success(`已填入${account.label}演示账号`)
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

      ElMessage.success('登录成功')
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
    <div class="auth-form login-form">
      <header class="form-header">
        <h2>用户登录</h2>
        <p class="form-desc">请使用您的账号密码登录系统</p>
      </header>

      <section class="test-accounts" aria-label="演示账号快捷填入">
        <div class="test-accounts-head">
          <span class="test-accounts-title">演示账号</span>
          <span class="test-accounts-hint">一键填入</span>
        </div>
        <div class="test-accounts-list">
          <button
            v-for="account in testAccounts"
            :key="account.role"
            type="button"
            class="test-account-btn"
            :style="{ '--role-color': account.color }"
            @click="fillTestAccount(account)"
          >
            <span class="role-name">{{ account.label }}</span>
            <span class="role-hint">演示</span>
          </button>
        </div>
      </section>

      <el-divider class="form-divider" />

      <el-form :model="loginForm" label-width="72px" label-position="right" class="auth-el-form">
        <el-form-item label="角色">
          <el-radio-group v-model="loginForm.userRole">
            <el-radio value="user">患者</el-radio>
            <el-radio value="doctor">医生</el-radio>
            <el-radio value="pharmacist">药师</el-radio>
            <el-radio value="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="loginForm.userAccount" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.userPassword"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item class="submit-item">
          <el-button
            type="primary"
            :loading="loading"
            class="submit-btn"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-switch-link">
        <span>还没有账号？</span>
        <router-link to="/auth/register" class="link-primary">立即注册</router-link>
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

.test-accounts {
  background: #f8fafb;
  border: 1px solid #e8eff4;
  border-radius: 10px;
  padding: 14px 16px;
}

.test-accounts-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.test-accounts-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.test-accounts-hint {
  font-size: 12px;
  color: #94a3b8;
}

.test-accounts-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.test-account-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 6px 12px;
  font-size: 13px;
  font-family: inherit;
  color: var(--role-color);
  background: #fff;
  border: 1px solid color-mix(in srgb, var(--role-color) 35%, #e2e8f0);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.test-account-btn:hover {
  background: color-mix(in srgb, var(--role-color) 6%, #fff);
  border-color: var(--role-color);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--role-color) 12%, transparent);
}

.test-account-btn:focus-visible {
  outline: 2px solid var(--role-color);
  outline-offset: 2px;
}

.role-name {
  font-weight: 500;
}

.role-hint {
  font-size: 11px;
  padding: 1px 5px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--role-color) 10%, #f1f5f9);
  color: #64748b;
}

.form-divider {
  margin: 20px 0;
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

@media (prefers-reduced-motion: reduce) {
  .test-account-btn {
    transition: none;
  }
}
</style>
