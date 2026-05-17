<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { House, User, UserFilled, Calendar, Document, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const menuItems = [
  { path: '/admin/dashboard', title: '首页', icon: House },
  { path: '/admin/users', title: '用户管理', icon: User },
  { path: '/admin/doctors', title: '医生管理', icon: UserFilled },
  { path: '/admin/schedules', title: '排班管理', icon: Calendar },
  { path: '/admin/appointments', title: '预约管理', icon: Document }
]

const handleLogout = () => {
  authStore.clearUserInfo()
  router.push('/auth/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <h2>🏥 管理端</h2>
      </div>
      <el-menu
        :default-active="route.path"
        class="menu"
        router
        background-color="#0f766e"
        text-color="#ffffff"
        active-text-color="#ffffff"
      >
        <el-menu-item 
          v-for="item in menuItems" 
          :key="item.path" 
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h3>{{ route.meta.title || '管理端' }}</h3>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" :src="''" style="margin-right: 8px; background: #14b8a6;" />
              <span>{{ authStore.userName || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100%;
}

.aside {
  background: #0f766e;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.logo h2 {
  color: white;
  font-size: 18px;
  margin: 0;
}

.menu {
  border: none;
}

.header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left h3 {
  margin: 0;
  color: #1f2937;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #374151;
}

.main {
  background: #f3f4f6;
  padding: 24px;
}
</style>
