import axios from 'axios';
import { httpClient } from './httpClient';
import type { LoginRequest, RefreshRequest, TokenResponse } from '../types/api';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/** Login uses a bare axios call (no Bearer) so interceptors do not attach a stale token. */
export async function login(request: LoginRequest): Promise<TokenResponse> {
  const { data } = await axios.post<TokenResponse>(`${baseURL}/api/v1/auth/login`, request);
  return data;
}

export async function refresh(request: RefreshRequest): Promise<TokenResponse> {
  const { data } = await axios.post<TokenResponse>(`${baseURL}/api/v1/auth/refresh`, request);
  return data;
}

export async function logout(): Promise<void> {
  try {
    await httpClient.post('/api/v1/auth/logout');
  } catch {
    // Client still discards tokens even if the server call fails (offline / expired).
  }
}
