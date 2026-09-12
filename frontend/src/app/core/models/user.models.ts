import { Role } from './auth.models';

export interface UserPageMetadata {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface UserPageResponse<T> {
  content: T[];
  page?: UserPageMetadata;
  number?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

export interface ManagedUser {
  id: number;
  email: string;
  role: Role;
  verified: boolean;
  enabled: boolean;
  twoFactorEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserSearchParams {
  search?: string;
  role?: Role | '';
  enabled?: boolean | null;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

export interface CreateUserRequest {
  email: string;
  password: string;
  role: Role;
}

export interface UpdateUserRequest {
  email: string;
  role: Role;
}

export interface UserStatusRequest {
  enabled: boolean;
}
