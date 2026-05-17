import { defineStore } from "pinia";
import { ref, computed } from "vue";

interface UserInfo {
  id?: number;
  userAccount?: string;
  userName?: string;
  userRole?: "user" | "doctor" | "admin";
  token?: string;
  [key: string]: any;
}

export const useAuthStore = defineStore("auth", () => {
  // 状态
  const userInfo = ref<UserInfo | null>(null);
  const token = ref<string>("");

  // 从localStorage初始化
  const initFromStorage = () => {
    const savedToken = localStorage.getItem("satoken");
    const savedUserInfo = localStorage.getItem("userInfo");

    if (savedToken) {
      token.value = savedToken;
    }

    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo);
      } catch (error) {
        console.error("解析用户信息失败:", error);
      }
    }
  };

  // Getters
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value);
  const userRole = computed(() => userInfo.value?.userRole || "");
  const userName = computed(() => userInfo.value?.userName || "");

  // Actions
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info;
    if (info.token) {
      token.value = info.token;
      localStorage.setItem("satoken", info.token);
    }
    localStorage.setItem("userInfo", JSON.stringify(info));
  };

  const clearUserInfo = () => {
    userInfo.value = null;
    token.value = "";
    localStorage.removeItem("satoken");
    localStorage.removeItem("userInfo");
  };

  // 初始化
  initFromStorage();

  return {
    userInfo,
    token,
    isLoggedIn,
    userRole,
    userName,
    setUserInfo,
    clearUserInfo,
  };
});
