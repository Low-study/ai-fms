import apiClient from './client';
import type { Finding } from '../types/finding';
import type { PageResult } from '../types';

export const findingApi = {
  list: (params: { keyword?: string; page?: number; size?: number }) =>
    apiClient.get<PageResult<Finding>>('/findings', { params }),
  getById: (id: string) => apiClient.get<Finding>(`/findings/${id}`),
};
