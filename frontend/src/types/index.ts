// ── API Types ──

/** Unified API response from backend. */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/** Paginated response. */
export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

/** Pagination request params. */
export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  order?: 'asc' | 'desc';
}

// ── Domain Types (placeholders — to be extended per module) ──

export interface User {
  id: string;
  username: string;
  email: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface Role {
  id: string;
  name: string;
  description: string;
  createdAt: string;
}

export interface Tenant {
  id: string;
  name: string;
  code: string;
  status: string;
  createdAt: string;
}
