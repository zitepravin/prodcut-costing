import { httpClient } from './httpClient';

export interface PlatformCustomer {
  customerId: string;
  customerCode: string;
  customerName: string;
  status: string;
}

export interface PlatformModule {
  moduleId: string;
  moduleCode: string;
  moduleName: string;
  description: string | null;
  status: string;
}

export interface Entitlement {
  entitlementId: string;
  customerId: string;
  moduleId: string;
  moduleCode: string | null;
  enabled: boolean;
  effectiveFrom: string | null;
  effectiveTo: string | null;
}

export interface Membership {
  membershipId: string;
  customerId: string;
  userId: string;
  status: string;
}

export interface CustomerAccess {
  customerId: string;
  customerCode: string;
  customerName: string;
  enabledModuleCodes: string[];
  roleCodes: string[];
}

export interface EffectiveAccess {
  userId: string;
  username: string;
  customers: CustomerAccess[];
}

export async function fetchMyAccess(): Promise<EffectiveAccess> {
  const { data } = await httpClient.get<EffectiveAccess>('/api/v1/me/access');
  return data;
}

export async function listCustomers(): Promise<PlatformCustomer[]> {
  const { data } = await httpClient.get<PlatformCustomer[]>('/api/v1/platform/customers');
  return data;
}

export async function getCustomer(customerId: string): Promise<PlatformCustomer> {
  const { data } = await httpClient.get<PlatformCustomer>(`/api/v1/platform/customers/${customerId}`);
  return data;
}

export async function createCustomer(body: {
  customerCode: string;
  customerName: string;
}): Promise<PlatformCustomer> {
  const { data } = await httpClient.post<PlatformCustomer>('/api/v1/platform/customers', body);
  return data;
}

export async function listModules(): Promise<PlatformModule[]> {
  const { data } = await httpClient.get<PlatformModule[]>('/api/v1/platform/modules');
  return data;
}

export async function listCustomerModules(customerId: string): Promise<Entitlement[]> {
  const { data } = await httpClient.get<Entitlement[]>(
    `/api/v1/platform/customers/${customerId}/modules`,
  );
  return data;
}

export async function setEntitlement(body: {
  customerId: string;
  moduleCode: string;
  enabled: boolean;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
}): Promise<Entitlement> {
  const { data } = await httpClient.post<Entitlement>('/api/v1/platform/entitlements', body);
  return data;
}

export async function listCustomerUsers(customerId: string): Promise<Membership[]> {
  const { data } = await httpClient.get<Membership[]>(
    `/api/v1/platform/customers/${customerId}/users`,
  );
  return data;
}

export async function addMembership(body: {
  customerId: string;
  userId: string;
}): Promise<Membership> {
  const { data } = await httpClient.post<Membership>('/api/v1/platform/memberships', body);
  return data;
}

export async function updateCustomer(
  customerId: string,
  body: { customerName: string },
): Promise<PlatformCustomer> {
  const { data } = await httpClient.put<PlatformCustomer>(
    `/api/v1/platform/customers/${customerId}`,
    body,
  );
  return data;
}

export async function setCustomerStatus(
  customerId: string,
  status: string,
): Promise<PlatformCustomer> {
  const { data } = await httpClient.put<PlatformCustomer>(
    `/api/v1/platform/customers/${customerId}/status`,
    { status },
  );
  return data;
}

export async function deactivateMembership(membershipId: string): Promise<Membership> {
  const { data } = await httpClient.put<Membership>(
    `/api/v1/platform/memberships/${membershipId}/deactivate`,
  );
  return data;
}
