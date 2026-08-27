import { httpClient } from './httpClient';

export interface CostingVariant {
  variantId: string;
  variantCode: string;
  variantName: string;
  status: string;
  valuationStrategy: string;
  materialPriceSource: string;
  activityPriceSource: string;
  quantityStructureSource: string;
  currencyCode: string;
  lotSize: number | string;
  scrapPct: number | string;
  yieldPct: number | string;
  overheadPoolCode?: string;
  costComponentProfile?: string;
  costingSheetCode?: string;
  description?: string;
}

export async function listCostingVariants(): Promise<CostingVariant[]> {
  const { data } = await httpClient.get<CostingVariant[]>('/api/v1/costing-variants');
  return data;
}

export async function createCostingVariant(body: Record<string, unknown>): Promise<CostingVariant> {
  const { data } = await httpClient.post<CostingVariant>('/api/v1/costing-variants', body);
  return data;
}

export async function activateCostingVariant(variantId: string): Promise<CostingVariant> {
  const { data } = await httpClient.post<CostingVariant>(`/api/v1/costing-variants/${variantId}/activate`);
  return data;
}

export async function retireCostingVariant(variantId: string): Promise<CostingVariant> {
  const { data } = await httpClient.post<CostingVariant>(`/api/v1/costing-variants/${variantId}/retire`);
  return data;
}
