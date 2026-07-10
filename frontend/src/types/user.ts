export type UserStatus = 'ACTIVE' | 'LOCKED' | 'DISABLED' | 'DELETED';

export interface User {
  id: string;
  username: string;
  email: string;
  displayName?: string;
  phone?: string;
  status: UserStatus;
  tenantId?: string;
  lastLoginAt?: string;
  passwordChangedAt?: string;
  lockedUntil?: string;
  failedLoginCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
  phone?: string;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  password?: string;
  displayName?: string;
  phone?: string;
  status?: UserStatus;
}
