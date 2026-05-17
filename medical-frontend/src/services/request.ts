import { get, post } from "@/request";

export default function request<T = any>(
  url: string,
  options: any = {}
): Promise<any> {
  const method = options.method || "GET";
  const data = options.data;
  const params = options.params;

  if (method.toUpperCase() === "GET") {
    return get(url, { ...options, params });
  } else {
    return post(url, data, options);
  }
}
