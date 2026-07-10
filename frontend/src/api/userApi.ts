import apiClient, { type PaginatedData } from './client';
import type { User, CreateUserRequest, UpdateUserRequest } from '../types/user';

export const userApi = {
  list: (params: { keyword?: string; page: number; size: number }) =>
    apiClient.get<PaginatedData<User>>('/users', { params }),

  getById: (id: string) =>
    apiClient.get<User>(`/users/${id}`),

  create: (data: CreateUserRequest) =>
    apiClient.post<User>('/users', data),

  update: (id: string, data: UpdateUserRequest) =>
    apiClient.put<User>(`/users/${id}`, data),

  patch: (id: string, data: UpdateUserRequest) =>
    apiClient.patch<User>(`/users/${id}`, data),

  delete: (id: string) =>
    apiClient.delete(`/users/${id}`),
};
