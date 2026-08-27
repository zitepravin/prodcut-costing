import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  isAccessTokenLikelyExpired,
  saveSession,
  getUsername,
} from '../auth/tokenStorage';
import type { ApiErrorResponse, TokenResponse } from '../types/api';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const httpClient = axios.create({
  baseURL,
  timeout: 30_000,
});

// Default JSON for non-FormData requests (set per request so FormData is never polluted)
httpClient.defaults.headers.common['Accept'] = 'application/json';

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearSession();
    return null;
  }
  try {
    const { data } = await axios.post<TokenResponse>(`${baseURL}/api/v1/auth/refresh`, {
      refreshToken,
    });
    const username = getUsername() ?? 'user';
    saveSession(data.accessToken, data.refreshToken, data.expiresInMs, username);
    return data.accessToken;
  } catch {
    clearSession();
    return null;
  }
}

httpClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  const isForm =
    typeof FormData !== 'undefined' && config.data instanceof FormData;

  if (!isForm) {
    if (!config.headers) {
      config.headers = {} as InternalAxiosRequestConfig['headers'];
    }
    if (!config.headers['Content-Type'] && !config.headers['content-type']) {
      config.headers['Content-Type'] = 'application/json';
    }
  }

  let token = getAccessToken();
  if (token && isAccessTokenLikelyExpired()) {
    if (!refreshPromise) {
      refreshPromise = refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
    }
    token = await refreshPromise;
  }
  if (token) {
    if (!config.headers) {
      config.headers = {} as InternalAxiosRequestConfig['headers'];
    }
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Tenant context for module entitlement (Central Access). Backend is authoritative.
  try {
    const customerId =
      typeof localStorage !== 'undefined' ? localStorage.getItem('pcs.activeCustomerId') : null;
    if (customerId && customerId.trim()) {
      if (!config.headers) {
        config.headers = {} as InternalAxiosRequestConfig['headers'];
      }
      config.headers['X-Customer-Id'] = customerId.trim();
    }
  } catch {
    /* ignore storage errors */
  }
  return config;
});

/** V224 — support session banner signal (READ_ONLY). */
let supportReadOnlyListeners: Array<(active: boolean) => void> = [];

export function onSupportReadOnlyChange(listener: (active: boolean) => void): () => void {
  supportReadOnlyListeners.push(listener);
  return () => {
    supportReadOnlyListeners = supportReadOnlyListeners.filter((l) => l !== listener);
  };
}

function notifySupportReadOnly(active: boolean) {
  supportReadOnlyListeners.forEach((l) => l(active));
}

httpClient.interceptors.response.use(
  (response) => {
    const flag = response.headers?.['x-support-read-only'];
    if (flag === 'true' || flag === true) {
      notifySupportReadOnly(true);
    }
    return response;
  },
  async (error: AxiosError<ApiErrorResponse>) => {
    const original = error.config;
    if (error.response?.status === 401 && original && !(original as { _retry?: boolean })._retry) {
      (original as { _retry?: boolean })._retry = true;
      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
      }
      const newToken = await refreshPromise;
      if (newToken) {
        if (original.headers) {
          original.headers.Authorization = `Bearer ${newToken}`;
        }
        return httpClient(original);
      }
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiErrorResponse | Blob | string | undefined;
    if (data && typeof data === 'object' && !(data instanceof Blob) && 'message' in data && data.message) {
      const api = data as ApiErrorResponse;
      if (api.fieldErrors?.length) {
        const fields = api.fieldErrors.map((f) => `${f.field}: ${f.message}`).join('; ');
        return `${api.message} (${fields})`;
      }
      return String(api.message);
    }
    // CSV/blob downloads: server may return JSON error body as Blob
    if (typeof Blob !== 'undefined' && data instanceof Blob) {
      // Synchronous fallback — caller may still only see status
      if (error.response?.status === 403) {
        return 'Access denied (403). You may lack the required role for this export.';
      }
      if (error.response?.status === 404) {
        return 'Report not found (404).';
      }
      if (error.response?.status && error.response.status >= 500) {
        return `Report export failed (${error.response.status}). Try again or narrow program scope.`;
      }
    }
    if (typeof data === 'string' && data.trim()) return data;
    if (error.response?.status) {
      return `Request failed (${error.response.status})${error.message ? `: ${error.message}` : ''}`;
    }
    if (error.message) return error.message;
  }
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred';
}
