const ACCESS_KEY = 'pcs.accessToken';
const REFRESH_KEY = 'pcs.refreshToken';
const EXPIRES_AT_KEY = 'pcs.expiresAt';
const USERNAME_KEY = 'pcs.username';

function storageGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function storageSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Private mode / quota — session will not persist across reloads.
  }
}

function storageRemove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // ignore
  }
}

export function saveSession(
  accessToken: string,
  refreshToken: string,
  expiresInMs: number,
  username: string,
): void {
  const expiresAt = Date.now() + expiresInMs - 5_000;
  storageSet(ACCESS_KEY, accessToken);
  storageSet(REFRESH_KEY, refreshToken);
  storageSet(EXPIRES_AT_KEY, String(expiresAt));
  storageSet(USERNAME_KEY, username);
}

export function clearSession(): void {
  storageRemove(ACCESS_KEY);
  storageRemove(REFRESH_KEY);
  storageRemove(EXPIRES_AT_KEY);
  storageRemove(USERNAME_KEY);
}

export function getAccessToken(): string | null {
  return storageGet(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return storageGet(REFRESH_KEY);
}

export function getUsername(): string | null {
  return storageGet(USERNAME_KEY);
}

export function isAccessTokenLikelyExpired(): boolean {
  const raw = storageGet(EXPIRES_AT_KEY);
  if (!raw) return true;
  const n = Number(raw);
  return !Number.isFinite(n) || Date.now() >= n;
}

export function hasSession(): boolean {
  return Boolean(getAccessToken() && getRefreshToken());
}

/** Decode JWT payload without verifying signature (UI only — server validates). */
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length < 2 || !parts[1]) return null;
    const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);
    const json = atob(padded);
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export function extractAuthorities(token: string | null): string[] {
  if (!token) return [];
  const payload = decodeJwtPayload(token);
  if (!payload) return [];
  const auth = payload.authorities ?? payload.roles ?? payload.scope;
  if (Array.isArray(auth)) {
    return auth.map(String);
  }
  if (typeof auth === 'string') {
    return auth.split(/[,\s]+/).filter(Boolean);
  }
  return [];
}
