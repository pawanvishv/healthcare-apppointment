import { apiClient } from "@/lib/http/client";
import type {
  AuthTokenResponse,
  LoginRequest,
  RegisterRequest,
} from "@/types/api";

export async function login(data: LoginRequest): Promise<AuthTokenResponse> {
  return apiClient<AuthTokenResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(data),
    skipAuth: true,
  });
}

export async function register(data: RegisterRequest): Promise<AuthTokenResponse> {
  return apiClient<AuthTokenResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify(data),
    skipAuth: true,
  });
}

export async function logout(refreshToken: string): Promise<void> {
  return apiClient<void>("/auth/logout", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  });
}
