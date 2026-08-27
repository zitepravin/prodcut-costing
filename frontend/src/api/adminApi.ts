import { httpClient } from './httpClient';

export interface AdminUser {
  userId: string;
  username: string;
  email: string;
  status: string;
  mfaEnabled: boolean;
  roleCodes: string[];
  lastLoginAt: string | null;
}

export interface AdminRole {
  roleId: string;
  roleCode: string;
  description: string | null;
  systemRole: boolean;
}

export async function listAdminUsers(): Promise<AdminUser[]> {
  const { data } = await httpClient.get<AdminUser[]>('/api/v1/admin/users');
  return data;
}

export async function createAdminUser(body: {
  username: string;
  email: string;
  password: string;
  roleCodes: string[];
}): Promise<AdminUser> {
  const { data } = await httpClient.post<AdminUser>('/api/v1/admin/users', body);
  return data;
}

export async function updateUserRoles(userId: string, roleCodes: string[]): Promise<AdminUser> {
  const { data } = await httpClient.put<AdminUser>(`/api/v1/admin/users/${userId}/roles`, {
    roleCodes,
  });
  return data;
}

export async function deactivateUser(userId: string): Promise<AdminUser> {
  const { data } = await httpClient.put<AdminUser>(`/api/v1/admin/users/${userId}/deactivate`);
  return data;
}

export async function reactivateUser(userId: string): Promise<AdminUser> {
  const { data } = await httpClient.put<AdminUser>(`/api/v1/admin/users/${userId}/reactivate`);
  return data;
}

export async function listAdminRoles(): Promise<AdminRole[]> {
  const { data } = await httpClient.get<AdminRole[]>('/api/v1/admin/roles');
  return data;
}

export async function createAdminRole(body: {
  roleCode: string;
  description?: string;
}): Promise<AdminRole> {
  const { data } = await httpClient.post<AdminRole>('/api/v1/admin/roles', body);
  return data;
}
