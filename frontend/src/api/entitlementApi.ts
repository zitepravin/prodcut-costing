import { httpClient } from './httpClient';

export type CatalogRow = {
  permissionCode: string;
  moduleCode: string;
  featureCode: string;
  functionCode: string;
  functionName: string;
  description: string | null;
};

export type EntitlementRow = {
  permissionCode: string;
  moduleCode: string;
  featureCode: string;
  functionName: string;
  platformAllowed: boolean;
  locked: boolean;
};

export type RolePermissionRow = {
  permissionCode: string;
  moduleCode: string;
  featureCode: string;
  functionName: string;
  platformAllowed: boolean;
  roleAllowed: boolean;
  locked: boolean;
  effectiveAllowed: boolean;
};

export async function listPermissionCatalog(): Promise<CatalogRow[]> {
  const { data } = await httpClient.get<CatalogRow[]>('/api/v1/platform/permission-catalog');
  return data;
}

export async function listPlatformEntitlements(customerId: string): Promise<EntitlementRow[]> {
  const { data } = await httpClient.get<EntitlementRow[]>(
    `/api/v1/platform/customers/${encodeURIComponent(customerId)}/permission-entitlements`,
  );
  return data;
}

export async function setPlatformEntitlement(
  customerId: string,
  permissionCode: string,
  allowed: boolean,
): Promise<EntitlementRow> {
  const { data } = await httpClient.put<EntitlementRow>(
    `/api/v1/platform/customers/${encodeURIComponent(customerId)}/permission-entitlements/${encodeURIComponent(permissionCode)}`,
    { allowed },
  );
  return data;
}

export async function listRolePermissions(roleCode: string): Promise<RolePermissionRow[]> {
  const { data } = await httpClient.get<RolePermissionRow[]>('/api/v1/customer/role-permissions', {
    params: { roleCode },
  });
  return data;
}

export async function setRolePermission(
  roleCode: string,
  permissionCode: string,
  allowed: boolean,
): Promise<RolePermissionRow> {
  const { data } = await httpClient.put<RolePermissionRow>(
    `/api/v1/customer/role-permissions/${encodeURIComponent(permissionCode)}`,
    { allowed },
    { params: { roleCode } },
  );
  return data;
}
