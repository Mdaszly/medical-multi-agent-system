import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/stores/auth";

// 获取对应角色的首页
export const getHomeRoute = (role: string) => {
  const roleHomeMap: Record<string, string> = {
    user: "/patient/home",
    doctor: "/doctor/dashboard",
    admin: "/admin/dashboard",
  };
  return roleHomeMap[role] || "/auth/login";
};

// 简单占位组件
const PlaceholderComponent = {
  template: '<div style="padding: 20px">功能开发中...</div>',
};

const routes: RouteRecordRaw[] = [
  // 公开路由
  {
    path: "/auth",
    redirect: "/auth/login",
    children: [
      {
        path: "login",
        name: "Login",
        component: () => import("@/views/auth/Login.vue"),
        meta: { title: "登录", requiresAuth: false },
      },
      {
        path: "register",
        name: "Register",
        component: () => import("@/views/auth/Register.vue"),
        meta: { title: "注册", requiresAuth: false },
      },
    ],
  },

  // 患者端路由
  {
    path: "/patient",
    redirect: "/patient/home",
    meta: { requiresAuth: true, roles: ["user"] },
    component: () => import("@/components/layout/PatientLayout.vue"),
    children: [
      {
        path: "home",
        name: "PatientHome",
        component: () => import("@/views/patient/Home.vue"),
        meta: { title: "首页" },
      },
      {
        path: "doctors",
        name: "Doctors",
        component: () => import("@/views/patient/Doctors.vue"),
        meta: { title: "医生列表" },
      },
      {
        path: "doctor/:id",
        name: "DoctorDetail",
        component: () => import("@/views/patient/DoctorDetail.vue"),
        meta: { title: "医生详情" },
      },
      {
        path: "booking/:doctorId",
        name: "Booking",
        component: () => import("@/views/patient/Booking.vue"),
        meta: { title: "预约挂号" },
      },
      {
        path: "my-appointments",
        name: "MyAppointments",
        component: () => import("@/views/patient/MyAppointments.vue"),
        meta: { title: "我的预约" },
      },
      {
        path: "prescriptions",
        name: "Prescriptions",
        component: () => import("@/views/patient/Prescriptions.vue"),
        meta: { title: "我的处方" },
      },
      {
        path: "payments",
        name: "Payments",
        component: () => import("@/views/patient/Payments.vue"),
        meta: { title: "我的账单" },
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("@/views/patient/Profile.vue"),
        meta: { title: "个人中心" },
      },
      {
        path: "consult",
        name: "Consult",
        component: () => import("@/views/patient/ConsultPage.vue"),
        meta: { title: "线上问诊" },
      },
      {
        path: "consult/:sessionId",
        name: "ConsultSession",
        component: () => import("@/views/patient/ConsultSessionPage.vue"),
        meta: { title: "问诊详情" },
      },
    ],
  },

  // 医生端路由
  {
    path: "/doctor",
    redirect: "/doctor/dashboard",
    meta: { requiresAuth: true, roles: ["doctor"] },
    component: () => import("@/components/layout/DoctorLayout.vue"),
    children: [
      {
        path: "dashboard",
        name: "DoctorDashboard",
        component: () => import("@/views/doctor/Dashboard.vue"),
        meta: { title: "工作台" },
      },
      {
        path: "schedule",
        name: "DoctorSchedule",
        component: () => import("@/views/doctor/Schedule.vue"),
        meta: { title: "排班管理" },
      },
      {
        path: "appointments",
        name: "DoctorAppointments",
        component: () => import("@/views/doctor/Appointments.vue"),
        meta: { title: "接诊列表" },
      },
      {
        path: "prescription/:appointmentId?",
        name: "DoctorPrescription",
        component: () => import("@/views/doctor/Prescription.vue"),
        meta: { title: "开具处方" },
      },
      {
        path: "appointment/:id",
        name: "DoctorAppointmentDetail",
        component: () => import("@/views/doctor/AppointmentDetail.vue"),
        meta: { title: "预约详情" },
      },
    ],
  },

  // 管理员端路由
  {
    path: "/admin",
    redirect: "/admin/dashboard",
    meta: { requiresAuth: true, roles: ["admin"] },
    component: () => import("@/components/layout/AdminLayout.vue"),
    children: [
      {
        path: "dashboard",
        name: "AdminDashboard",
        component: () => import("@/views/admin/Dashboard.vue"),
        meta: { title: "首页" },
      },
      {
        path: "users",
        name: "AdminUsers",
        component: () => import("@/views/admin/UserManagement.vue"),
        meta: { title: "用户管理" },
      },
      {
        path: "doctors",
        name: "AdminDoctors",
        component: () => import("@/views/admin/DoctorManagement.vue"),
        meta: { title: "医生管理" },
      },
      {
        path: "schedules",
        name: "AdminSchedules",
        component: () => import("@/views/admin/ScheduleManagement.vue"),
        meta: { title: "排班管理" },
      },
      {
        path: "appointments",
        name: "AdminAppointments",
        component: () => import("@/views/admin/AppointmentManagement.vue"),
        meta: { title: "预约管理" },
      },
    ],
  },

  // 重定向
  {
    path: "/",
    redirect: "/auth/login",
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  // 设置页面标题
  let pageTitle = to.meta.title || "医疗门诊系统";
  document.title = pageTitle + " - 医疗门诊系统";

  // 检查是否需要登录
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return "/auth/login";
  }

  // 如果已登录，访问登录/注册页面则跳转到对应首页
  if (
    authStore.isLoggedIn &&
    (to.path === "/auth/login" || to.path === "/auth/register")
  ) {
    return getHomeRoute(authStore.userRole);
  }

  // 检查角色权限
  if (to.meta.roles && Array.isArray(to.meta.roles)) {
    if (!to.meta.roles.includes(authStore.userRole)) {
      return getHomeRoute(authStore.userRole);
    }
  }

  return true;
});

export default router;
