export const ROLES = ['ADMIN', 'EMPLOYEE'] as const;
export type Role = (typeof ROLES)[number];

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string | null;
  tokenType: string | null;
  expiresIn: number;
  requiresTwoFactor: boolean;
  challengeId: string | null;
  message: string;
}

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginVerificationRequest {
  challengeId: string;
  otp: string;
}

export interface RecoveryRequest {
  email: string;
}

export interface RecoveryVerificationRequest {
  email: string;
  otp: string;
  newPassword: string;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface TwoFactorRequest {
  currentPassword: string;
}

export interface ChallengeVerificationRequest {
  challengeId: string;
  otp: string;
}

export interface UserResponse {
  id: number;
  email: string;
  role: Role;
  verified: boolean;
  enabled?: boolean;
  twoFactorEnabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface StoredSession {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
  user: UserResponse | null;
}
