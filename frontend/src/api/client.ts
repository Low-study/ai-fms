import axios from 'axios';
import type { AxiosResponse } from 'axios';

/**
 * Unified API response from backend.
 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * Paginated data from backend.
 */
export interface PaginatedData<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

/**
 * Axios instance pre-configured for the backend API.
 */
const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Request interceptor ──

apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ── Response interceptor ──

apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const result = response.data;
    if (result.code !== 0) {
      // 业务异常 — 抛出给调用方处理
      return Promise.reject(new ApiError(result.code, result.message));
    }
    // 解包 Result<T> 的 data 字段，调用方直接拿到业务数据
    response.data = result.data as unknown as AxiosResponse['data'];
    return response;
  },
  (error) => {
    // Network or HTTP error
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    // 从非 2xx 响应体中提取结构化错误信息
    const body = error.response?.data;
    if (body?.code && body?.message) {
      return Promise.reject(new ApiError(body.code, body.message));
    }
    return Promise.reject(error);
  },
);

/**
 * Business error thrown when API returns non-zero code.
 */
export class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
}

export default apiClient;
