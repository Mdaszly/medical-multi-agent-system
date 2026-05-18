import { ElMessage } from "element-plus";

export interface RequestOptions {
  [key: string]: any;
  showLoading?: boolean;
  showError?: boolean;
}

export interface BaseResponse<T = any> {
  code: number;
  data: T;
  message: string;
}

// 错误码映射
const ERROR_CODE_MAP: Record<number, string> = {
  40100: "请先登录",
  40101: "登录已过期",
  40300: "无访问权限",
  40401: "用户不存在",
  40402: "预约不存在",
  40403: "时段已满",
  50000: "服务器错误",
  50001: "服务暂不可用",
};

// 开发环境使用代理，生产环境使用完整URL
const getBaseUrl = () => {
  if (import.meta.env.DEV) {
    return ""; // 使用Vite代理
  }
  return import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
};

async function request<T = any>(
  url: string,
  options: RequestOptions = {},
): Promise<BaseResponse<T>> {
  const {
    showLoading = false,
    showError = true,
    params,
    ...fetchOptions
  } = options;
  const baseUrl = getBaseUrl();

  // 构建完整URL，处理GET请求参数
  let fullUrl = baseUrl + url;

  // 如果是GET请求且有params参数，则拼接到URL上
  if (params && Object.keys(params).length > 0) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.append(key, String(value));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      fullUrl += (fullUrl.includes("?") ? "&" : "?") + queryString;
    }
  }

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  // 从localStorage获取token
  const token = localStorage.getItem("satoken");
  if (token) {
    headers["Authorization"] = token;
  }

  const defaultOptions: RequestInit = {
    headers,
  };

  const mergedOptions = { ...defaultOptions, ...fetchOptions };

  try {
    const response = await fetch(fullUrl, mergedOptions);
    const data: BaseResponse<T> = await response.json();

    // 统一业务错误处理
    if (data.code !== 0) {
      // 401错误处理
      if (data.code === 40100 || data.code === 40101) {
        localStorage.removeItem("satoken");
        localStorage.removeItem("userInfo");
        window.location.href = "/auth/login";
      }

      const errorMsg = ERROR_CODE_MAP[data.code] || data.message || "请求失败";

      if (showError) {
        ElMessage.error(errorMsg);
      }

      throw new Error(errorMsg);
    }

    return data;
  } catch (error) {
    // 网络错误处理
    if (error instanceof TypeError && error.message.includes("fetch")) {
      const networkError = "网络连接失败，请检查网络";
      if (showError) {
        ElMessage.error(networkError);
      }
      throw new Error(networkError);
    }

    throw error;
  }
}

// GET请求封装
export function get<T = any>(url: string, options?: RequestOptions) {
  return request<T>(url, { ...options, method: "GET" });
}

// POST请求封装
export function post<T = any>(
  url: string,
  data?: any,
  options?: RequestOptions,
) {
  return request<T>(url, {
    ...options,
    method: "POST",
    body: data ? JSON.stringify(data) : undefined,
  });
}

export default request;
