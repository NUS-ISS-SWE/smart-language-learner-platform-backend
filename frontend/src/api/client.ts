import axios, { AxiosError } from "axios";
import { API_BASE_URL } from "@/lib/constants";
import { getStoredAccessToken } from "@/lib/auth-storage";
import type { ApiError } from "@/types/api";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getStoredAccessToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export function getApiErrorMessage(error: unknown) {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ApiError | string | undefined;

    if (typeof data === "string" && data.trim().length > 0) {
      return data;
    }

    if (typeof data === "object" && data?.message) {
      return data.message;
    }

    if (typeof data === "object" && data?.error) {
      return data.error;
    }

    if (error.message) {
      return error.message;
    }
  }

  return "Something went wrong. Please try again.";
}
