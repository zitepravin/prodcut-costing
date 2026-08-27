import { httpClient } from './httpClient';

export interface ImportCalcResponse {
  calculationId: string;
  lifecycleStatus: string;
  reviewStatus: string;
  currencyCode: string;
  invoiceValue: number | string | null;
  customsValue: number | string | null;
  totalLandedCost: number | string | null;
  unitLandedCost: number | string | null;
  quantity: number | string | null;
  missingData: string[];
  components: { componentCode: string; amount: number | string; formulaNote?: string }[];
}

const BASE = '/api/v1/import-costing';

export async function calculateImport(body: Record<string, unknown>): Promise<ImportCalcResponse> {
  const { data } = await httpClient.post<ImportCalcResponse>(`${BASE}/calculate`, body);
  return data;
}

export async function getImportCalc(id: string): Promise<ImportCalcResponse> {
  const { data } = await httpClient.get<ImportCalcResponse>(`${BASE}/${id}`);
  return data;
}

export async function lifecycleImport(id: string, action: string): Promise<ImportCalcResponse> {
  const { data } = await httpClient.post<ImportCalcResponse>(`${BASE}/${id}/${action.toLowerCase()}`, { action });
  return data;
}
