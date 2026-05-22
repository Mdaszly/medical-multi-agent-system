<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  getCurrentUser,
  updateProfile,
  changePassword
} from '@/services/medical/yonghuguanli'

const authStore = useAuthStore()
const activeTab = ref('info')
const profileLoading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profileForm = reactive({
  userAccount: authStore.userInfo?.userAccount || '',
  userName: authStore.userInfo?.userName || '',
  phone: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const loadProfile = async () => {
  profileLoading.value = true
  try {
    const res = await getCurrentUser()
    if (res.data) {
      profileForm.userAccount = res.data.userAccount || authStore.userInfo?.userAccount || ''
      profileForm.userName = res.data.userName || ''
      profileForm.phone = res.data.phone || ''
      profileForm.email = res.data.email || ''
    }
  } catch {
    // 错误已由 request 统一提示
  } finally {
    profileLoading.value = false
  }
}

const handleSaveProfile = async () => {
  const userName = profileForm.userName?.trim()
  if (!userName) {
    ElMessage.warning('请填写姓名')
    return
  }

  profileSaving.value = true
  try {
    const body: API.UserUpdateRequest = { userName }
    const phone = profileForm.phone?.trim()
    const email = profileForm.email?.trim()
    if (phone) body.phone = phone
    if (email) body.email = email

    const res = await updateProfile(body)
    if (res.data) {
      authStore.setUserInfo({
        ...authStore.userInfo,
        userName: res.data.userName,
        userAccount: res.data.userAccount || profileForm.userAccount
      })
      profileForm.userName = res.data.userName || userName
      profileForm.phone = res.data.phone || phone
      profileForm.email = res.data.email || email
    }
    ElMessage.success(res.message || '个人信息已保存')
  } catch {
    // 错误已由 request 统一提示
  } finally {
    profileSaving.value = false
  }
}

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  passwordSaving.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success(res.message || '密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    // 错误已由 request 统一提示
  } finally {
    passwordSaving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="profile-page">
    <el-card v-loading="profileLoading">
      <template #header>
        <span>个人中心</span>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="个人信息" name="info">
          <el-form :model="profileForm" label-width="100px" style="max-width: 500px; margin: 0 auto;">
            <el-form-item label="账号">
              <el-input v-model="profileForm.userAccount" disabled />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="profileForm.userName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="passwordForm" label-width="100px" style="max-width: 500px; margin: 0 auto;">
            <el-form-item label="当前密码">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（8-20位，含大小写字母和数字）" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 900px;
}
</style>
