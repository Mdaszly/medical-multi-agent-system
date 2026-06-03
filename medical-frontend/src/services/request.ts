import { get, post, put } from "@/request";

export default function request<T = any>(
  url: string,
  options: any = {}
): Promise<any> {
  const method = (options.method || "GET").toUpperCase();
  const data = options.data;
  const params = options.params;

  if (method === "GET") {
    return get(url, { ...options, params });
  }
  if (method === "PUT") {
    return put(url, data, options);
  }
  return post(url, data, options);
}
