<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { House, User, Document, SwitchButton, ChatDotRound, OfficeBuilding, FirstAidKit } from '@element-plus/icons-vue'
import NotificationBell from '@/components/notification/NotificationBell.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const menuItems = [
  { path: '/patient/home', title: '首页', icon: House },
  { path: '/patient/doctors', title: '预约挂号', icon: OfficeBuilding },
  { path: '/patient/my-appointments', title: '我的预约', icon: Document },
  { path: '/patient/consult', title: '线上问诊', icon: ChatDotRound },
  { path: '/patient/health-profile', title: '健康档案', icon: FirstAidKit },
  { path: '/patient/prescriptions', title: '我的处方', icon: Document },
  { path: '/patient/payments', title: '我的账单', icon: Document },
  { path: '/patient/profile', title: '个人中心', icon: User },
]

const handleLogout = () => {
  authStore.clearUserInfo()
  router.push('/auth/login')
}
</script>

<template>
  <el-container class="patient-layout">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <h2 class="logo-text">医疗门诊系统</h2>
        <span class="logo-sub">智慧医疗服务</span>
      </div>
      <el-menu
        :default-active="route.path"
        class="menu patient-side-menu"
        router
        background-color="#06b6d4"
        text-color="#ffffff"
        active-text-color="#ffffff"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
          class="nav-item-standard"
          :class="{ 'nav-item-consult': item.path === '/patient/consult' }"
        >
          <el-icon aria-hidden="true"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h3>{{ route.meta.title || '医疗门诊系统' }}</h3>
        </div>
        <div class="header-right">
          <NotificationBell role="user" />
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" :src="''" style="margin-right: 8px; background: #14b8a6;" />
              <span>{{ authStore.userName || '用户' }}</span>
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
.patient-layout {
  height: 100%;
}

.aside {
  background: linear-gradient(180deg, #0891b2 0%, #06b6d4 48%, #0e7490 100%);
  box-shadow: 2px 0 12px rgba(8, 145, 178, 0.15);
}

.logo {
  padding: 18px 16px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.logo-text {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 4px;
  letter-spacing: 0.04em;
}

.logo-sub {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.08em;
}

.menu {
  border: none;
  padding-top: 4px;
}

.patient-side-menu :deep(.nav-item-standard) {
  margin: 2px 8px;
  border-radius: 8px;
  min-height: 44px;
  transition: background 0.2s ease;
}

.patient-side-menu :deep(.nav-item-standard:hover) {
  background: rgba(255, 255, 255, 0.12) !important;
}

.patient-side-menu :deep(.nav-item-standard.is-active) {
  background: rgba(255, 255, 255, 0.2) !important;
  font-weight: 600;
}

/* 侧栏问诊项：轻量高亮，主入口在首页 */
.patient-side-menu :deep(.nav-item-consult.is-active) {
  background: rgba(255, 255, 255, 0.25) !important;
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
