import { env } from "@/lib/env";
import { useAuthStore } from "@/store/auth-store";
import type { ApiError, ApiResponse } from "@/types/api";

export class HttpError extends Error {
  status: number;
  errorCode: string;
  details?: string[];

  constructor(apiError: ApiError) {
    super(apiError.message);
    this.name = "HttpError";
    this.status = apiError.status;
    this.errorCode = apiError.errorCode;
    this.details = apiError.details;
  }
}

let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken, setSession, clearSession } = useAuthStore.getState();
  if (!refreshToken) return null;

  try {
    const res = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) {
      clearSession();
      return null;
    }

    const json: ApiResponse<{
      accessToken: string;
      refreshToken: string;
    }> = await res.json();

    setSession({
      accessToken: json.data.accessToken,
      refreshToken: json.data.refreshToken,
    });

    return json.data.accessToken;
  } catch {
    clearSession();
    return null;
  }
}

interface RequestOptions extends RequestInit {
  skipAuth?: boolean;
}

export async function apiClient<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { skipAuth = false, headers: customHeaders, ...rest } = options;
  const { accessToken } = useAuthStore.getState();

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(customHeaders as Record<string, string>),
  };

  if (!skipAuth && accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const url = `${env.NEXT_PUBLIC_API_BASE_URL}${path}`;

  const doFetch = async (token?: string | null) => {
    const h = { ...headers };
    if (!skipAuth && token) {
      h.Authorization = `Bearer ${token}`;
    }
    return fetch(url, { ...rest, headers: h });
  };

  let response = await doFetch(accessToken);

  if ((response.status === 401 || response.status === 403) && !skipAuth) {
    if (!isRefreshing) {
      isRefreshing = true;
      refreshPromise = refreshAccessToken().finally(() => {
        isRefreshing = false;
        refreshPromise = null;
      });
    }

    const newToken = await (refreshPromise ?? refreshAccessToken());
    if (newToken) {
      response = await doFetch(newToken);
    }
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const json = await response.json();

  if (!response.ok || json.success === false) {
    const error: ApiError = json.error ?? {
      timestamp: json.timestamp ?? new Date().toISOString(),
      path: json.path ?? path,
      status: json.status ?? response.status,
      errorCode: json.errorCode ?? json.error ?? "UNKNOWN",
      message:
        json.message ??
        (typeof json.error === "string" ? json.error : null) ??
        (response.statusText || "Request failed"),
    };
    throw new HttpError(error);
  }

  return json.data as T;
}
