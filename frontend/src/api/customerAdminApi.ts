import { httpClient } from './httpClient';

export interface CustomerUser {
  userId: string;
  employeeName: string | null;
  email: string;
  username: string;
  employeeId: string | null;
  departmentId: string | null;
  departmentName: string | null;
  designation: string | null;
  mobileNumber: string | null;
  status: string;
  roleCodes: string[];
  lastLoginAt: string | null;
  createdAt: string | null;
}

export interface Department {
  departmentId: string;
  customerId: string;
  departmentCode: string;
  departmentName: string;
  description: string | null;
  status: string;
}

export async function listCustomerUsers(): Promise<CustomerUser[]> {
  const { data } = await httpClient.get<CustomerUser[]>('/api/v1/customer/users');
  return data;
}

export async function createCustomerUser(body: {
  employeeName: string;
  email: string;
  employeeId?: string;
  departmentId?: string;
  designation?: string;
  mobileNumber?: string;
  roleCodes: string[];
}) {
  const { data } = await httpClient.post('/api/v1/customer/users', body);
  return data;
}

export async function updateCustomerUser(
  userId: string,
  body: {
    employeeName?: string;
    employeeId?: string;
    departmentId?: string;
    designation?: string;
    mobileNumber?: string;
    roleCodes?: string[];
  },
) {
  const { data } = await httpClient.put<CustomerUser>(`/api/v1/customer/users/${userId}`, body);
  return data;
}

export async function activateCustomerUser(userId: string) {
  const { data } = await httpClient.post<CustomerUser>(`/api/v1/customer/users/${userId}/activate`);
  return data;
}

export async function deactivateCustomerUser(userId: string) {
  const { data } = await httpClient.post<CustomerUser>(`/api/v1/customer/users/${userId}/deactivate`);
  return data;
}

export async function listDepartments(): Promise<Department[]> {
  const { data } = await httpClient.get<Department[]>('/api/v1/customer/departments');
  return data;
}

export async function createDepartment(body: {
  departmentCode: string;
  departmentName: string;
  description?: string;
}) {
  const { data } = await httpClient.post<Department>('/api/v1/customer/departments', body);
  return data;
}

export async function updateDepartment(
  departmentId: string,
  body: { departmentName?: string; description?: string; status?: string },
) {
  const { data } = await httpClient.put<Department>(`/api/v1/customer/departments/${departmentId}`, body);
  return data;
}

export async function deactivateDepartment(departmentId: string) {
  const { data } = await httpClient.post<Department>(
    `/api/v1/customer/departments/${departmentId}/deactivate`,
  );
  return data;
}

export async function listAssignableRoles(): Promise<string[]> {
  const { data } = await httpClient.get<string[]>('/api/v1/customer/roles/assignable');
  return data;
}

export async function rolePermissions(roleCode: string): Promise<string[]> {
  const { data } = await httpClient.get<string[]>(`/api/v1/customer/roles/${roleCode}/permissions`);
  return data;
}

export async function acceptInvitation(token: string, password: string) {
  const { data } = await httpClient.post('/api/v1/customer/invitations/accept', { token, password });
  return data;
}

/** Commercial license usage (read-only for Customer Admin). */
export interface RoleUsageRow {
  roleCode: string;
  used: number;
  max: number;
  overLimit: boolean;
}

export interface LicenseUsageSnapshot {
  customerId: string;
  licenseStatus: string | null;
  planCode?: string;
  totalUserLimit: number | null;
  activeUsers: number;
  plantSiteLimit?: number;
  plantSiteUsed?: number;
  plantSiteOverLimit?: boolean;
  validFrom?: string;
  validTo?: string;
  graceEndDate?: string;
  overLimit: boolean;
  licenseVersion?: number;
  roleUsage: RoleUsageRow[];
}

export async function fetchLicenseUsage(): Promise<LicenseUsageSnapshot> {
  const { data } = await httpClient.get<LicenseUsageSnapshot>('/api/v1/commercial-license/usage');
  return data;
}
