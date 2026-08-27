import { httpClient } from './httpClient';
import type { MaterialMaster, MaterialSearchParams, PageResponse } from '../types/api';

export interface CreateMaterialRequest {
  code: string;
  description?: string;
  rate: string;
  uom: string;
  plantId?: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  grade?: string;
  specificationStandard?: string;
  density: string;
  scrapRecoveryRate?: string | null;
  materialForm?: string;
  indexLinkedFlag?: boolean;
  indexCode?: string | null;
  basePrice?: string | null;
  alloySurchargePerUnit?: string | null;
}

export async function searchMaterials(params: MaterialSearchParams = {}): Promise<PageResponse<MaterialMaster>> {
  const { data } = await httpClient.get<PageResponse<MaterialMaster>>('/api/v1/master/materials', {
    params: {
      q: params.q || undefined,
      status: params.status || undefined,
      plantId: params.plantId || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: params.sort ?? 'code,asc',
    },
  });
  return data;
}

export async function getMaterialById(id: string): Promise<MaterialMaster> {
  const { data } = await httpClient.get<MaterialMaster>(`/api/v1/master/materials/${id}`);
  return data;
}


export async function createMaterial(request: CreateMaterialRequest): Promise<MaterialMaster> {
  const { data } = await httpClient.post<MaterialMaster>('/api/v1/master/materials', {
    ...request,
    indexLinkedFlag: request.indexLinkedFlag ?? false,
  });
  return data;
}


export interface BulkImportResult {
  totalRows: number;
  succeeded: number;
  failed: number;
  errors: Array<{ rowNumber: number; reason: string }>;
}

export async function listMaterialVersions(code: string): Promise<MaterialMaster[]> {
  const { data } = await httpClient.get<MaterialMaster[]>(
    `/api/v1/master/materials/by-code/${encodeURIComponent(code)}/versions`,
  );
  return data;
}

export async function bulkImportMaterials(file: File): Promise<BulkImportResult> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await httpClient.post<BulkImportResult>('/api/v1/master/bulk-import/materials', form);
  return data;
}
