import { httpClient } from './httpClient';

export interface CostObject {
  costObjectId: string;
  customerId: string;
  objectType: string;
  objectCode: string;
  objectName: string;
  currencyCode: string;
  uom?: string | null;
  referenceId?: string | null;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CalculateResponse {
  calculationId: string;
  costObjectId: string;
  costMethod: string;
  calculationDate: string;
  currencyCode: string;
  technicalTotal: number | string | null;
  commercialTotal: number | string | null;
  totalCost: number | string | null;
  unitCost: number | string | null;
  sellingPrice: number | string | null;
  quantity: number | string | null;
  allocatedIndirect?: number | string | null;
  lifecycleStatus: string;
  industryProfile?: string | null;
  createdAt?: string;
}

export interface NormResponse {
  normId: string;
  customerId: string;
  normCode: string;
  description?: string | null;
  unit?: string | null;
  parameterKey?: string | null;
  formulaCode?: string | null;
  normValue: number | string;
  objectScope?: string | null;
  objectReferenceId?: string | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  versionNo: number;
  active: boolean;
  approvalStatus: string;
  source?: string | null;
  createdAt?: string;
}

export interface SobResponse {
  sobId: string;
  customerId: string;
  costObjectId?: string | null;
  productCode?: string | null;
  supplierId: string;
  allocationPercent: number | string;
  allocationBasis: string;
  allocationGroupCode: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  status: string;
  priority: number;
  versionNo: number;
  createdAt?: string;
}

export interface IndustryProfile {
  profileId: string;
  profileCode: string;
  profileName: string;
  description?: string | null;
  defaultMethodsJson?: string | null;
  defaultComponentsJson?: string | null;
  defaultDriversJson?: string | null;
  active: boolean;
}

const BASE = '/api/v1/universal-costing';

export async function listCostObjects(): Promise<CostObject[]> {
  const { data } = await httpClient.get<CostObject[]>(`${BASE}/cost-objects`);
  return data;
}

export async function createCostObject(body: {
  objectType: string;
  objectCode: string;
  objectName: string;
  currencyCode?: string;
  uom?: string;
  referenceId?: string;
}): Promise<CostObject> {
  const { data } = await httpClient.post<CostObject>(`${BASE}/cost-objects`, body);
  return data;
}

export async function calculate(body: Record<string, unknown>): Promise<CalculateResponse> {
  const { data } = await httpClient.post<CalculateResponse>(`${BASE}/calculations`, body);
  return data;
}

export async function listCalculations(costObjectId?: string): Promise<CalculateResponse[]> {
  const { data } = await httpClient.get<CalculateResponse[]>(`${BASE}/calculations`, {
    params: costObjectId ? { costObjectId } : undefined,
  });
  return data;
}

export async function transitionLifecycle(
  calculationId: string,
  action: string,
  actorUserId?: string,
): Promise<CalculateResponse> {
  const { data } = await httpClient.post<CalculateResponse>(
    `${BASE}/calculations/${calculationId}/lifecycle`,
    { action, actorUserId },
  );
  return data;
}

export async function listNorms(): Promise<NormResponse[]> {
  const { data } = await httpClient.get<NormResponse[]>(`${BASE}/norms`);
  return data;
}

export async function createNorm(body: Record<string, unknown>): Promise<NormResponse> {
  const { data } = await httpClient.post<NormResponse>(`${BASE}/norms`, body);
  return data;
}

export async function approveNorm(normId: string, actorUserId?: string): Promise<NormResponse> {
  const { data } = await httpClient.post<NormResponse>(`${BASE}/norms/${normId}/approve`, null, {
    params: actorUserId ? { actorUserId } : undefined,
  });
  return data;
}

export async function createNormVersion(
  priorNormId: string,
  body: Record<string, unknown>,
): Promise<NormResponse> {
  const { data } = await httpClient.post<NormResponse>(`${BASE}/norms/${priorNormId}/versions`, body);
  return data;
}

export async function listSob(): Promise<SobResponse[]> {
  const { data } = await httpClient.get<SobResponse[]>(`${BASE}/sob`);
  return data;
}

export async function createSob(
  body: Record<string, unknown>,
  allowPartial = false,
): Promise<SobResponse> {
  const { data } = await httpClient.post<SobResponse>(`${BASE}/sob`, body, {
    params: { allowPartial },
  });
  return data;
}

export async function listIndustryProfiles(): Promise<IndustryProfile[]> {
  const { data } = await httpClient.get<IndustryProfile[]>(`${BASE}/industry-profiles`);
  return data;
}
