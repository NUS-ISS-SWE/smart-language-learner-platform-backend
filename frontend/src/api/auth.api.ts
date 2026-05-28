import { apiClient } from "@/api/client";
import type {
  AppUser,
  ForgotPasswordPayload,
  JwtAccessToken,
  LoginPayload,
  RegisterPayload,
} from "@/features/auth/types";

export async function login(payload: LoginPayload) {
  const response = await apiClient.post<JwtAccessToken>("/api/v1/auth/login", payload);
  return response.data;
}

export async function register(payload: RegisterPayload) {
  const response = await apiClient.post<AppUser>("/api/v1/auth/register", payload);
  return response.data;
}

export async function forgotPassword(payload: ForgotPasswordPayload) {
  await apiClient.post<void>("/api/v1/auth/forgot-password", payload);
}
