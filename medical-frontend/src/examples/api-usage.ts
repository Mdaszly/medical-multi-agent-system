import { login, getCurrentUser1, logout } from "./services/medical";

export async function exampleLogin() {
  const result = await login({
    userAccount: "admin",
    userPassword: "123456",
  });

  if (result.code === 0) {
    console.log("登录成功:", result.data);
    return result.data;
  } else {
    console.error("登录失败:", result.message);
  }
}

export async function exampleGetCurrentUser() {
  const result = await getCurrentUser1();

  if (result.code === 0) {
    console.log("当前用户:", result.data);
    return result.data;
  } else {
    console.error("获取用户信息失败:", result.message);
  }
}

export async function exampleLogout() {
  const result = await logout();

  if (result.code === 0) {
    console.log("退出登录成功");
  } else {
    console.error("退出登录失败:", result.message);
  }
}
