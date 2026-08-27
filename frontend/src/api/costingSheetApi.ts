import { httpClient } from './httpClient';

export interface CostingSheet {
  sheetId: string;
  sheetCode: string;
  sheetName: string;
  status: string;
  plantCode?: string;
  currencyCode: string;
  ruleCount: number;
}

export async function listCostingSheets(): Promise<CostingSheet[]> {
  const { data } = await httpClient.get<CostingSheet[]>('/api/v1/costing-sheets');
  return data;
}

export async function createCostingSheet(body: Record<string, unknown>): Promise<CostingSheet> {
  const { data } = await httpClient.post<CostingSheet>('/api/v1/costing-sheets', body);
  return data;
}

export async function addCostingSheetRule(sheetId: string, body: Record<string, unknown>) {
  const { data } = await httpClient.post(`/api/v1/costing-sheets/${sheetId}/rules`, body);
  return data;
}

export async function activateCostingSheet(sheetId: string): Promise<CostingSheet> {
  const { data } = await httpClient.post<CostingSheet>(`/api/v1/costing-sheets/${sheetId}/activate`);
  return data;
}

export async function applyCostingSheet(body: {
  sheetId: string;
  material: string;
  process: string;
  tooling?: string;
  packaging?: string;
}) {
  const { data } = await httpClient.post('/api/v1/costing-sheets/apply', body);
  return data as {
    sheetCode: string;
    lines: Array<Record<string, unknown>>;
    baseSubtotal: string | number;
    totalOverhead: string | number;
    grandTotal: string | number;
    calculationBasis: string;
  };
}
